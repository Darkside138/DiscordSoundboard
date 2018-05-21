package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.PlayEvent;
import net.dirtydeeds.discordsoundboard.beans.PlayEventFilenameCount;
import net.dirtydeeds.discordsoundboard.beans.PlayEventUsernameCount;
import net.dirtydeeds.discordsoundboard.beans.PlayEventUsernameFilenameCount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.UUID;

public interface PlayEventRepository extends CrudRepository<PlayEvent, UUID> {

    @Query(value = "select p.username as username, count(p.id) as count from PlayEvent p group by p.username order by count(p.id) desc")
    Collection<PlayEventUsernameCount> getUsernameCount();

    @Query(value = "select p.filename as filename, count(p.id) as count from PlayEvent p group by p.filename order by count(p.id) desc")
    Collection<PlayEventFilenameCount> getFilenameCount();

    @Query(value = "select p.username as username, p.filename as filename, count(p.id) as count from PlayEvent p group by p.username, p.filename order by count(p.id) desc")
    Collection<PlayEventUsernameFilenameCount> getUsernameFilenameCount();
}
