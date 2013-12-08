# ACWAVAB
Android Controlled Wireless Active-Vision Autobot

A Raspberry Pi robot generates a Wi-Fi hotspot with a Wi-Fi USB Adapter. An Android device (preferably tablet)
running ACVAWAB app connects and controls the robot by making an SSH connection. Video transmission is currently
done using a separate phone connected to the same network as the app. This will be later updated to using
Raspberry Pi camera with encoding and transmission through UDP socks.

#Depends:

Bounty Castle Provider: bcprov-jdk15on-148.jar

Java Secure Channel: jsch-0.1.50.jar

Simple Logging Facade for Java (SLF4J): slf4j-simple-1.7.5.jar

Bounty Castle PKIX(Non-Provider): bcpkix-jdk15on-148.jar

Configurable Java ping library: jpingy0_1-alpha.jar

Simple Logging Facade for Java (SLF4J): slf4j-api-1.7.5.jar