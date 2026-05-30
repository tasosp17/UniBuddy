UniBuddy is a context-aware Android application prototype developed as my Bachelor's Thesis. It is designed to enhance the social and academic experience of university students by facilitating peer-to-peer communication based on physical proximity.

Instead of relying on battery-draining GPS, UniBuddy utilizes a privacy-first recognition algorithm that checks existing university WiFi identifiers (SSIDs) to detect on-campus presence.

-- KEY FEATURES --
*Smart campus presence: detects if a student is on campus by verifying the active WiFi SSID against the university's network, ensuring zero battery drain compared to GPS solutions
*3-Tier privacy control: users have dynamic control over their visibility status using a custom framework (Visible to: None / Best Buddies / Everyone).
*Real time communication: instant peer-to-peer and group messaging synchronized instantly across devices
*Academic tools: automated lecture reminders before classes, groupchats per course, students attending same course and a direct email interface to contact university staff

-- TECH STACK --
Developed in Kotlin.
Google Firebase is used as BaaS (Authentication, Realtime Database, Cloud Messaging).
Coroutines used for asynchronous operations.
HTTP requests via Volley.
UI/UX was built with XML layouts and Material Design components (recyclerviews, sliders, etc)

Architecture Note - FCM Installation
To achieve a fully serverless prototype for the scope of the thesis, the OAuth 2.0 credential generation for Firebase Cloud Messaging (FCM) was implemented directly on the Android client. 
I am fully aware that storing a Firebase Service Account key client-side is a severe security risk in a production environment. In a real-world commercial deployment, this logic would be refactored and moved to a secure backend server (e.g., Firebase Cloud Functions or a Node.js server) to prevent credential exposure via app decompilation. The private keys have been intentionally omitted from this public repository.

-- How to run locally -- 
To run this project on your local machine, you will need to provide your own Firebase configuration:
1. Clone the repository.
2. Create a Firebase project and add an Android app.
3. Download the `google-services.json` file and place it in the `app/` directory.
4. Generate a new private key from Firebase Project Settings -> Service Accounts.
5. Place the key in the appropriate raw resources folder and update the referencing path in the code.
6. Build and run via Android Studio.

SCREENSHOTS

<img width="350" height="771" alt="image" src="https://github.com/user-attachments/assets/2bad55dc-5703-4227-ba11-646234d6a77a" />
<img width="459" height="767" alt="image" src="https://github.com/user-attachments/assets/d0c46e15-5e57-47d0-8580-53a271edd678" />
<img width="345" height="768" alt="image" src="https://github.com/user-attachments/assets/b497570c-35ae-4298-a962-fd6bd59210ca" />
<img width="347" height="767" alt="image" src="https://github.com/user-attachments/assets/524a1c95-684c-4d63-b1a7-7e6fbfe612a5" />







