package com.xiaoying.studio.task;

import com.xiaoying.studio.asset.Asset;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TaskMapper {
    String COLUMNS = "t.id, t.user_id, t.template_id, p.title AS template_title, t.status, t.current_stage, "
            + "t.failure_stage, t.failure_reason, t.result_url, t.simulation_failure_stage, t.simulation_delay_ms, "
            + "t.created_at, t.updated_at, t.finished_at, t.result_storage_key, t.result_mime_type";

    @Insert("""
        INSERT INTO production_task (id, user_id, template_id, simulation_failure_stage, simulation_delay_ms)
        VALUES (#{id}, #{userId}, #{templateId}, #{failureStage}, #{delayMs})
        """)
    void insert(@Param("id") String id, @Param("userId") String userId, @Param("templateId") String templateId,
            @Param("failureStage") Stage failureStage, @Param("delayMs") int delayMs);

    @Insert("INSERT INTO task_asset (task_id, asset_id, slot_index) VALUES (#{taskId}, #{assetId}, #{slot})")
    void addAsset(@Param("taskId") String taskId, @Param("assetId") String assetId, @Param("slot") int slot);

    @Select("SELECT " + COLUMNS + " FROM production_task t JOIN template p ON p.id=t.template_id WHERE t.id=#{id}")
    TaskRow find(String id);

    @Select("SELECT " + COLUMNS + " FROM production_task t JOIN template p ON p.id=t.template_id WHERE t.id=#{id} AND t.user_id=#{userId}")
    TaskRow findOwned(@Param("id") String id, @Param("userId") String userId);

    @Select("<script>SELECT " + COLUMNS + """
         FROM production_task t JOIN template p ON p.id=t.template_id WHERE t.user_id=#{userId}
         <if test="beforeTime != null">
         AND (t.created_at &lt; #{beforeTime} OR (t.created_at = #{beforeTime} AND t.id &lt; #{beforeId}))
         </if>
         ORDER BY t.created_at DESC, t.id DESC LIMIT #{limit}
         </script>
        """)
    List<TaskRow> list(@Param("userId") String userId, @Param("beforeTime") Instant beforeTime,
            @Param("beforeId") String beforeId, @Param("limit") int limit);

    @Select("""
        SELECT a.id, a.user_id, a.original_name, a.storage_key, a.mime_type, a.size_bytes, a.created_at
        FROM task_asset ta JOIN upload_asset a ON a.id=ta.asset_id WHERE ta.task_id=#{taskId} ORDER BY ta.slot_index
        """)
    List<Asset> assets(String taskId);

    @Select("""
        SELECT stage, status, started_at, finished_at, error_message FROM task_step WHERE task_id=#{taskId}
        ORDER BY FIELD(stage, 'VALIDATE', 'GENERATE', 'FINALIZE')
        """)
    List<TaskStep> steps(String taskId);

    @Update("UPDATE production_task SET status='RUNNING' WHERE id=#{id} AND status='QUEUED'")
    int claim(String id);

    @Update("UPDATE production_task SET current_stage=#{stage} WHERE id=#{id} AND status='RUNNING'")
    int setStage(@Param("id") String id, @Param("stage") Stage stage);

    @Insert("INSERT INTO task_step (task_id, stage, status) VALUES (#{id}, #{stage}, 'RUNNING')")
    void startStep(@Param("id") String id, @Param("stage") Stage stage);

    @Update("UPDATE task_step SET status='SUCCEEDED', finished_at=CURRENT_TIMESTAMP(3) WHERE task_id=#{id} AND stage=#{stage} AND status='RUNNING'")
    int finishStep(@Param("id") String id, @Param("stage") Stage stage);

    @Update("""
        UPDATE production_task SET status='SUCCEEDED', result_url=#{url}, result_storage_key=#{key},
        result_mime_type=#{mime}, finished_at=CURRENT_TIMESTAMP(3)
        WHERE id=#{id} AND status='RUNNING' AND current_stage='FINALIZE'
        """)
    int succeed(@Param("id") String id, @Param("url") String url, @Param("key") String key, @Param("mime") String mime);

    @Update("""
        UPDATE production_task SET status='FAILED', failure_stage=#{stage}, failure_reason=#{reason},
        finished_at=CURRENT_TIMESTAMP(3) WHERE id=#{id} AND status IN ('QUEUED','RUNNING')
        """)
    int fail(@Param("id") String id, @Param("stage") Stage stage, @Param("reason") String reason);

    @Update("""
        UPDATE task_step SET status='FAILED', error_message=#{reason}, finished_at=CURRENT_TIMESTAMP(3)
        WHERE task_id=#{id} AND status='RUNNING'
        """)
    void failRunningStep(@Param("id") String id, @Param("reason") String reason);

    @Update("""
        UPDATE task_step SET status='FAILED', error_message='服务重启，任务已中止，请重新提交', finished_at=CURRENT_TIMESTAMP(3)
        WHERE status='RUNNING'
        """)
    void interruptSteps();

    @Update("""
        UPDATE production_task SET status='FAILED', failure_stage=current_stage,
        failure_reason='服务重启，任务已中止，请重新提交', finished_at=CURRENT_TIMESTAMP(3)
        WHERE status IN ('QUEUED','RUNNING')
        """)
    int interruptTasks();
}
