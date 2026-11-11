package com.xiaoying.studio.catalog;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TemplateMapper {
    @Select("""
        <script>
        SELECT id, title, category, description, cover_url, required_image_count, max_file_size_bytes
        FROM template
        <where><if test="category != null and category != ''">category = #{category}</if></where>
        ORDER BY sort_order, id
        </script>
        """)
    List<Template> findAll(@Param("category") String category);

    @Select("SELECT id, title, category, description, cover_url, required_image_count, max_file_size_bytes FROM template WHERE id = #{id}")
    Template findById(@Param("id") String id);

    @Select("SELECT DISTINCT category FROM template ORDER BY category")
    List<String> findCategories();
}
