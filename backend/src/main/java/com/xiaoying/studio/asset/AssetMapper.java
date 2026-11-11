package com.xiaoying.studio.asset;

import org.apache.ibatis.annotations.*;

@Mapper
public interface AssetMapper {
    @Insert("""
        INSERT INTO upload_asset (id, user_id, original_name, storage_key, mime_type, size_bytes, created_at)
        VALUES (#{id}, #{userId}, #{originalName}, #{storageKey}, #{mimeType}, #{sizeBytes}, #{createdAt})
        """)
    void insert(Asset asset);

    @Select("SELECT * FROM upload_asset WHERE id = #{id} AND user_id = #{userId}")
    Asset findOwned(@Param("id") String id, @Param("userId") String userId);
}
