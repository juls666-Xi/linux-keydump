# PhotoTaker Instructions

## Overview

This project allows you to remotely control an Android device's camera from a web browser. It consists of three parts:

1.  **`phototaker.java`**: An Android application that accesses the camera and connects to a WebSocket server.
2.  **`remote_connector.js`**: A Node.js WebSocket server that acts as a bridge between the Android app and the web UI.
3.  **`phototaker-ui.html`**: A web-based user interface to send commands to the Android app.

## Setup

1.  **Install Node.js and npm:** If you don't have them, install them from [https://nodejs.org/](https://nodejs.org/).
2.  **Install WebSocket library:** Open a terminal and run:
    ```bash
    npm install ws
    ```
3.  **Start the WebSocket server:** In the same terminal, run:
    ```bash
    node remote_connector.js
    ```
4.  **Open the Web UI:** Open the `phototaker-ui.html` file in your web browser.
5.  **Configure the Android App:**
    *   Open the `phototaker.java` file.
    *   Find the line `private static final String SERVER_URL = "ws://192.168.1.100:8080";`
    *   **Crucially, change `192.168.1.100` to the local IP address of the computer running the `remote_connector.js` server.** You can find your local IP by running `ifconfig` (Linux/macOS) or `ipconfig` (Windows) in your terminal.
6.  **Run the Android App:** Compile and run the `phototaker.java` code on an Android device that is connected to the same local network as your computer.

## Usage

1.  **Initialize:** Once the server is running and the Android app is active, click the **Initialize** button in the web UI. This will prepare the camera on the device.
2.  **Capture:** Click the **CAPTURE** button to take a photo.
3.  **View:** A confirmation and the size of the captured image will be displayed in the web UI's log console.

