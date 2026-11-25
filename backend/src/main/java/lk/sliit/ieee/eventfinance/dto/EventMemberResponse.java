package lk.sliit.ieee.eventfinance.dto;

import lk.sliit.ieee.eventfinance.entity.ChapterRole;

/** Limited event-assignment directory; never includes credentials or administrator flags. */
public record EventMemberResponse(Long id, String displayName, ChapterRole role,
        boolean enabled, boolean assigned) {}
