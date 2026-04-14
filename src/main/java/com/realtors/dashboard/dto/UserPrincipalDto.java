package com.realtors.dashboard.dto;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;

import java.util.Collection;
import java.util.Set;

@Data
public class UserPrincipalDto implements UserDetails {

    /**
	 * 
	 */
	private static final Logger logger = LoggerFactory.getLogger(UserPrincipalDto.class);
	private static final long serialVersionUID = 1L;
	private UUID userId;
	private UUID roleId;
    private Set<UserRole> roles;
    private int roleLevel;
    
    public UserPrincipalDto(UUID userId, Set<UserRole> roles, UUID roleId, int roleLevel) {
        this.userId = userId;
        this.roles = roles;
        this.roleId = roleId;
        this.roleLevel = roleLevel;
    }

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    public UUID getUserId() {
        return userId;
    }
    
    public UUID getRoleId() {
        return roleId;
    }
    
    public int getRoleLevel() {
        return roleLevel;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
            .toList();
    }

    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return userId.toString(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
