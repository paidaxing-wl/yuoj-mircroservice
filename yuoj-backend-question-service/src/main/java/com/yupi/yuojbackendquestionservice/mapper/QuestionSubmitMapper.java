package com.yupi.yuojbackendquestionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.yuojbackmodel.model.entity.QuestionSubmit;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;


/**
* @author lenovo
* @description 针对表【question_submit(题目提交)】的数据库操作Mapper
* @createDate 2026-09-14 20:35:07
* @Entity com.yuoj.model.entity.QuestionSubmit
*/
public interface QuestionSubmitMapper extends BaseMapper<QuestionSubmit> {

    /**
     * 状态原子更新：只有当前状态等于 expectStatus 时才更新，用于判题任务抢占。
     *
     * @return 受影响行数，1 表示抢占成功，0 表示状态已被别人改掉
     */
    @Update("UPDATE question_submit SET status = #{newStatus}, updateTime = NOW() "
            + "WHERE id = #{questionSubmitId} AND status = #{expectStatus} AND isDelete = 0")
    int compareAndSetStatus(@Param("questionSubmitId") long questionSubmitId,
                            @Param("expectStatus") Integer expectStatus,
                            @Param("newStatus") Integer newStatus);
}
