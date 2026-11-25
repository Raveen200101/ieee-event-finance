package lk.sliit.ieee.eventfinance.dto;
import lk.sliit.ieee.eventfinance.entity.ChapterRole;
public record LoginResponse(String accessToken, String tokenType, long expiresIn, ChapterRole role, UserResponse user) {}
