# SmartNotify – ML-Based Intelligent Notification Management System


SmartNotify is an AI/ML-based intelligent notification management system designed to reduce unnecessary smartphone interruptions while ensuring that important communications are not missed.

The system classifies incoming Android notifications into **LOW**, **MEDIUM**, and **HIGH** priority levels using Machine Learning. During Focus Mode, unnecessary notifications are suppressed while important notifications can override the focus session.

---

## 📖 1. Project Overview

Modern smartphone users receive a large number of notifications from messaging applications, social media, work applications, banking services, promotional services, and other sources.

Constant notifications can interrupt studying, meetings, work, and other activities that require concentration.

SmartNotify addresses this problem by combining:

- Machine Learning
- Android notification interception
- Focus Mode
- Context-aware decision logic
- Caller urgency triage
- User feedback
- Notification history and analytics

The system aims to provide a balance between **staying focused** and **not missing important communication**.

---

## 🎯 2. Problem Statement

Users often miss important calls or messages when they silence notifications during meetings, studying, work, or other focused activities.

Traditional silent or Do Not Disturb modes generally apply fixed rules and may suppress both unnecessary and important notifications.

SmartNotify provides an intelligent notification management system that identifies notification priority and selectively alerts the user when important communication requires attention.

---

## ✨ 3. Key Features

### 🔔 Intelligent Notification Classification

Incoming notifications are classified into three priority levels:

- `LOW`
- `MEDIUM`
- `HIGH`

### 🎯 Focus Mode

When Focus Mode is active:

| Priority | Action |
|---|---|
| LOW | Suppressed and stored in quiet history |
| MEDIUM | Soft notification alert |
| HIGH | Important alert override |

### 🚨 Urgency Detection

Strong urgency signals can influence the final notification priority.

Examples include:

- `immediately`
- `server down`
- `OTP`
- `security alert`
- 'emergency'
