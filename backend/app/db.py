import os
import sqlite3
import uuid
import time
from typing import List, Dict, Any, Optional

DB_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "data", "smartnotify.db")

def get_connection():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    """Initializes the SQLite database tables with Focus Session support."""
    conn = get_connection()
    cursor = conn.cursor()
    
    # Focus Sessions Table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS focus_sessions (
            session_id TEXT PRIMARY KEY,
            start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            end_time TIMESTAMP NULL,
            status TEXT NOT NULL DEFAULT 'ACTIVE'
        )
    """)

    # Notifications Table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS notifications (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id TEXT,
            app_name TEXT NOT NULL,
            sender TEXT DEFAULT '',
            title TEXT NOT NULL,
            message TEXT NOT NULL,
            priority TEXT NOT NULL,
            confidence REAL NOT NULL,
            model TEXT NOT NULL,
            action_taken TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    # Check & add missing columns for existing notifications table
    cursor.execute("PRAGMA table_info(notifications)")
    notif_cols = [row[1] for row in cursor.fetchall()]
    if "session_id" not in notif_cols:
        cursor.execute("ALTER TABLE notifications ADD COLUMN session_id TEXT DEFAULT 'default_session'")
    if "sender" not in notif_cols:
        cursor.execute("ALTER TABLE notifications ADD COLUMN sender TEXT DEFAULT ''")
    
    # Call Requests Table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS call_requests (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id TEXT,
            caller_name TEXT NOT NULL,
            phone_number TEXT NOT NULL,
            request_type TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'PENDING',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)

    cursor.execute("PRAGMA table_info(call_requests)")
    call_cols = [row[1] for row in cursor.fetchall()]
    if "session_id" not in call_cols:
        cursor.execute("ALTER TABLE call_requests ADD COLUMN session_id TEXT DEFAULT 'default_session'")
    
    # Settings Table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS settings (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        )
    """)
    
    # User Feedback Table for Personalization Layer
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS user_feedback (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            notification_id INTEGER NULL,
            target_name TEXT NOT NULL,
            rating TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)

    conn.commit()
    conn.close()

def add_user_feedback(target_name: str, rating: str, notification_id: Optional[int] = None) -> Dict[str, Any]:
    """Saves user feedback rating ('IMPORTANT' or 'NOT_IMPORTANT') to train personalization layer."""
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
        INSERT INTO user_feedback (notification_id, target_name, rating)
        VALUES (?, ?, ?)
    """, (notification_id, target_name, rating))
    fb_id = cursor.lastrowid
    conn.commit()
    cursor.execute("SELECT * FROM user_feedback WHERE id = ?", (fb_id,))
    row = cursor.fetchone()
    conn.close()
    return dict(row) if row else {"id": fb_id, "target_name": target_name, "rating": rating}


# Focus Session Management Functions
def create_new_focus_session() -> Dict[str, Any]:
    """Creates a new active Focus Session and completes any previously active sessions."""
    conn = get_connection()
    cursor = conn.cursor()
    
    # Close any open active sessions
    cursor.execute("UPDATE focus_sessions SET status = 'COMPLETED', end_time = CURRENT_TIMESTAMP WHERE status = 'ACTIVE'")
    
    session_id = f"session_{int(time.time())}_{uuid.uuid4().hex[:6]}"
    cursor.execute("INSERT INTO focus_sessions (session_id, status) VALUES (?, 'ACTIVE')", (session_id,))
    conn.commit()
    
    cursor.execute("SELECT * FROM focus_sessions WHERE session_id = ?", (session_id,))
    row = cursor.fetchone()
    conn.close()
    return dict(row) if row else {"session_id": session_id, "status": "ACTIVE"}

def get_active_focus_session() -> Optional[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY start_time DESC LIMIT 1")
    row = cursor.fetchone()
    conn.close()
    return dict(row) if row else None

def complete_active_focus_session() -> Optional[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY start_time DESC LIMIT 1")
    row = cursor.fetchone()
    if row:
        session_id = row['session_id']
        cursor.execute("UPDATE focus_sessions SET status = 'COMPLETED', end_time = CURRENT_TIMESTAMP WHERE session_id = ?", (session_id,))
        conn.commit()
        cursor.execute("SELECT * FROM focus_sessions WHERE session_id = ?", (session_id,))
        updated = cursor.fetchone()
        conn.close()
        return dict(updated) if updated else None
    conn.close()
    return None

def save_notification(app_name: str, title: str, message: str, priority: str, confidence: float, model: str, action_taken: str, sender: str = "", session_id: Optional[str] = None) -> Dict[str, Any]:
    conn = get_connection()
    cursor = conn.cursor()
    
    if not session_id:
        active = get_active_focus_session()
        session_id = active['session_id'] if active else 'default_session'

    cursor.execute("""
        INSERT INTO notifications (session_id, app_name, sender, title, message, priority, confidence, model, action_taken)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (session_id, app_name, sender, title, message, priority, confidence, model, action_taken))
    notif_id = cursor.lastrowid
    conn.commit()
    
    cursor.execute("SELECT * FROM notifications WHERE id = ?", (notif_id,))
    row = cursor.fetchone()
    conn.close()
    return dict(row) if row else {}

def get_session_notifications(session_id: str) -> List[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM notifications WHERE session_id = ? ORDER BY created_at DESC, id DESC", (session_id,))
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

def get_all_notifications(limit: int = 50) -> List[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM notifications ORDER BY created_at DESC, id DESC LIMIT ?", (limit,))
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

def clear_all_notifications():
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM notifications")
    cursor.execute("DELETE FROM call_requests")
    cursor.execute("DELETE FROM focus_sessions")
    conn.commit()
    conn.close()

def add_call_request(caller_name: str, phone_number: str, request_type: str, session_id: Optional[str] = None) -> Dict[str, Any]:
    conn = get_connection()
    cursor = conn.cursor()
    
    if not session_id:
        active = get_active_focus_session()
        session_id = active['session_id'] if active else 'default_session'

    cursor.execute("""
        INSERT INTO call_requests (session_id, caller_name, phone_number, request_type, status)
        VALUES (?, ?, ?, ?, 'PENDING')
    """, (session_id, caller_name, phone_number, request_type))
    req_id = cursor.lastrowid
    conn.commit()
    
    cursor.execute("SELECT * FROM call_requests WHERE id = ?", (req_id,))
    row = cursor.fetchone()
    conn.close()
    return dict(row) if row else {}

def get_session_call_requests(session_id: str) -> List[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM call_requests WHERE session_id = ? AND status != 'DISMISSED' ORDER BY created_at DESC", (session_id,))
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

def get_all_call_requests() -> List[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM call_requests WHERE status != 'DISMISSED' ORDER BY created_at DESC, id DESC")
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

def update_call_request_status(req_id: int, status: str) -> bool:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("UPDATE call_requests SET status = ? WHERE id = ?", (status, req_id))
    affected = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return affected

def get_session_summary_stats(session_id: str) -> Dict[str, int]:
    conn = get_connection()
    cursor = conn.cursor()
    
    cursor.execute("SELECT COUNT(*) FROM notifications WHERE session_id = ?", (session_id,))
    msg_count = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM call_requests WHERE session_id = ? AND status != 'DISMISSED'", (session_id,))
    call_count = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM notifications WHERE session_id = ? AND priority = 'HIGH'", (session_id,))
    high_count = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM notifications WHERE session_id = ? AND priority = 'MEDIUM'", (session_id,))
    med_count = cursor.fetchone()[0]

    cursor.execute("SELECT COUNT(*) FROM notifications WHERE session_id = ? AND priority = 'LOW'", (session_id,))
    low_count = cursor.fetchone()[0]
    
    conn.close()
    return {
        "messages": msg_count,
        "calls": call_count,
        "high": high_count,
        "medium": med_count,
        "low": low_count
    }
