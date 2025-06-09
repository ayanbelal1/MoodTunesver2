package com.musicapp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolHandler.class);
    
    public static void registerProtocolHandler() {
        String os = System.getProperty("os.name").toLowerCase();
        
        try {
            if (os.contains("win")) {
                registerWindowsProtocol();
            } else if (os.contains("mac")) {
                registerMacProtocol();
            } else if (os.contains("linux")) {
                registerLinuxProtocol();
            }
        } catch (Exception e) {
            logger.error("Failed to register protocol handler", e);
        }
    }
    
    private static void registerWindowsProtocol() throws IOException {
        String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
        String jarPath = new File(ProtocolHandler.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
        
        // Create registry entries using reg.exe
        String command = String.format(
            "REG ADD \"HKCU\\Software\\Classes\\musicapp\" /v \"URL Protocol\" /d \"\" /f && " +
            "REG ADD \"HKCU\\Software\\Classes\\musicapp\\shell\\open\\command\" /d \"\\\"%s\\\" -jar \\\"%s\\\" %%1\" /f",
            javaPath, jarPath
        );
        
        Runtime.getRuntime().exec(command);
    }
    
    private static void registerMacProtocol() throws IOException {
        String homeDir = System.getProperty("user.home");
        Path plistPath = Paths.get(homeDir, "Library", "LaunchAgents", "com.musicapp.plist");
        
        String plistContent = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>com.musicapp</string>
                <key>CFBundleURLTypes</key>
                <array>
                    <dict>
                        <key>CFBundleURLName</key>
                        <string>MusicApp Protocol</string>
                        <key>CFBundleURLSchemes</key>
                        <array>
                            <string>musicapp</string>
                        </array>
                    </dict>
                </array>
            </dict>
            </plist>
            """);
        
        Files.createDirectories(plistPath.getParent());
        Files.writeString(plistPath, plistContent);
    }
    
    private static void registerLinuxProtocol() throws IOException {
        String homeDir = System.getProperty("user.home");
        Path desktopPath = Paths.get(homeDir, ".local", "share", "applications", "musicapp-protocol.desktop");
        
        String desktopContent = String.format("""
            [Desktop Entry]
            Version=1.0
            Name=MusicApp Protocol Handler
            Exec=java -jar %s %%u
            Terminal=false
            Type=Application
            Categories=Application;
            MimeType=x-scheme-handler/musicapp;
            """, 
            new File(ProtocolHandler.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath()
        );
        
        Files.createDirectories(desktopPath.getParent());
        Files.writeString(desktopPath, desktopContent);
        
        // Register the protocol handler
        Runtime.getRuntime().exec("xdg-mime default musicapp-protocol.desktop x-scheme-handler/musicapp");
    }
} 