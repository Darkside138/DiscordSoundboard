package net.dirtydeeds.discordsoundboard.beans;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity representing a permission assigned to a role.
 * Allows runtime configuration of role-permission mappings.
 *
 * @author dfurrer
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Role name (admin, dj, moderator, user)
     */
    private String role;

    /**
     * Permission name (upload, delete-sounds, edit-sounds, manage-users, play-sounds, download-sounds, update-volume)
     */
    private String permission;

    /**
     * Timestamp when this permission was assigned
     */
    private Instant assignedAt;

    /**
     * User ID who assigned this permission
     */
    private String assignedBy;

    public RolePermission(String role, String permission) {
        this.role = role;
        this.permission = permission;
        this.assignedAt = Instant.now();
    }
}
