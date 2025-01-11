# Whiterun

**Whiterun** is a desktop application designed to simplify the management of custom Tiny Glade assets, enabling you to
modify in-game models effortlessly.

---

## 🔧 Setup Guide

### Requirements

1. Download the latest version of the JRE (Java Runtime Environment) [here](https://adoptium.net/) (version **21** or
   later).
2. Download the Whiterun JAR executable from
   the [Releases Page](https://github.com/Hbeau/whiterun/releases).

### Steps

1. Run the app by double-clicking the downloaded `.jar` file.
2. The application will attempt to automatically detect your Tiny Glade installation folder.
    - If the folder is not found, manually specify it using the "Browse..." button.
3. Click the **"Patch Game"** button.
    - *Note:* The app may take some time to respond.
    - If successful, you will see the `default.zip` entry in the list.
4. To add custom asset packages:
    - Open the `assets-pack` folder and paste your `.zip` files.
    - you can get your first one here: [windmill.zip](https://raw.githubusercontent.com/Hbeau/Whiterun/refs/heads/main/windmill.zip)
    - Click on the desired zip file in the list.
    - Confirm the pop-up and start your game.

---

## 🎨 Creating Your Own Assets

1. **Base Your Assets on the Default Pack:**
    - Copy the `default.zip` file and remove any unmodified files.
    - Add only the files you wish to change.

2. **Edit Models and Textures:**
    - Use the [TinyGlade Blender Add-On](https://github.com/Hbeau/TinyGlade-Blender-AddOn) to edit models.

3. **Compress the Assets:**
    - Ensure all files are zipped correctly to match the structure expected by Tiny Glade.

---

## 🤝 Contributing to Whiterun

Contributions are welcome!

### Upcoming Features

- Enhanced feedback and input controls.
- Improved UI for a better user experience.
- Background tasks with progress indicators.

Feel free to submit pull requests, report bugs, or suggest features.

---

## 📥 Useful Links

- [Download JRE 21+](https://adoptium.net/)
- [Releases Page](https://github.com/Hbeau/Whiterun/releases)
- [TinyGlade Blender Add-On](https://github.com/Hbeau/TinyGlade-Blender-AddOn)

---
