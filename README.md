# Overview

Study Planner is an Android app that helps users organize assignments.
Users can enter a task name, course, and due date, view their tasks,
mark tasks complete, and delete tasks.

The app saves tasks on the device so they remain available after
closing and reopening it. It also checks for blank fields and invalid
dates before saving a new task.

My goal for this project was to learn how to build a mobile app using
Kotlin, create interactive screens, and save information locally.

[Software Demo Video](PASTE_YOUR_VIDEO_LINK_HERE)

# Development Environment

- Android Studio
- Android emulator
- Kotlin
- Jetpack Compose and Material 3 for the interface
- SharedPreferences for local storage
- Android's JSON library for converting task data

# Testing

I tested the following features in the Android emulator:

- Adding a task
- Marking a task complete
- Deleting a task
- Rejecting blank fields and invalid dates
- Keeping a task and its completion status after reopening the app

# Useful Websites

- [Android Developers](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose Documentation](https://developer.android.com/develop/ui/compose/documentation)

# Future Work

- Add reminders for upcoming due dates.
- Allow users to edit existing tasks.
- Add filters for courses and completion status.
- Replace the typed date field with a date picker.