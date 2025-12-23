package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing role-permission mappings.
 *
 * @author dfurrer
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /**
     * Find all permissions for a specific role
     */
    List<RolePermission> findByRole(String role);

    /**
     * Find a specific role-permission combination
     */
    RolePermission findByRoleAndPermission(String role, String permission);

    /**
     * Delete all permissions for a specific role
     */
    void deleteByRole(String role);

    /**
     * Delete a specific permission from a role
     */
    void deleteByRoleAndPermission(String role, String permission);
}
