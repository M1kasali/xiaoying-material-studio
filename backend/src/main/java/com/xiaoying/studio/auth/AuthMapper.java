package com.xiaoying.studio.auth;

import java.time.Instant;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AuthMapper {
    @Select("SELECT id, display_name FROM app_user WHERE id = #{id}")
    User findUser(String id);

    @Select("""
        SELECT u.id, u.display_name FROM user_session s JOIN app_user u ON u.id = s.user_id
        WHERE s.token_hash = #{tokenHash} AND s.expires_at > CURRENT_TIMESTAMP(3)
        """)
    User findSession(String tokenHash);

    @Insert("INSERT INTO user_session (token_hash, user_id, expires_at) VALUES (#{hash}, #{userId}, #{expiresAt})")
    void insertSession(@Param("hash") String hash, @Param("userId") String userId, @Param("expiresAt") Instant expiresAt);

    @Delete("DELETE FROM user_session WHERE token_hash = #{hash}")
    void deleteSession(String hash);

    @Delete("DELETE FROM user_session WHERE expires_at <= CURRENT_TIMESTAMP(3)")
    void deleteExpiredSessions();
}
