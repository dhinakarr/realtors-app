package com.realtors.common.service;

import java.util.UUID;

/**
* Stores request-specific audit data (IP, User Agent, User ID)
* using ThreadLocal to ensure thread-safety and easy access.
*/
public class AuditContext {
 // ThreadLocal ensures each thread gets its own copy of the variables.
 private static final ThreadLocal<String> IP_ADDRESS = new ThreadLocal<>();
 private static final ThreadLocal<String> USER_AGENT = new ThreadLocal<>();
 private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();

 // --- SETTERS (Used by the Filter/Interceptor) ---
 public static void setContext(String ip, String agent, UUID userId) {
     IP_ADDRESS.set(ip);
     USER_AGENT.set(agent);
     USER_ID.set(userId);
 }

 // --- GETTERS (Used by the Service Layer) ---
 public static String getIpAddress() {
     return IP_ADDRESS.get();
 }

 public static String getUserAgent() {
     return USER_AGENT.get();
 }

 public static UUID getUserId() {
     // Handle case where user may not be authenticated (e.g., public API call)
     return USER_ID.get(); 
 }

 // --- CLEANUP (Crucial! Used by the Filter/Interceptor) ---
 public static void clear() {
     IP_ADDRESS.remove();
     USER_AGENT.remove();
     USER_ID.remove();
 }
}
