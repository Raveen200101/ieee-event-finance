package lk.sliit.ieee.eventfinance.repository;

import lk.sliit.ieee.eventfinance.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {}
