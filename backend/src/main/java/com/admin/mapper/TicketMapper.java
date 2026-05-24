package com.admin.mapper;

import com.admin.entity.Ticket;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TicketMapper {

    @Select("SELECT * FROM ticket ORDER BY create_time DESC")
    List<Ticket> findAll();

    @Select("SELECT * FROM ticket WHERE id = #{id}")
    Ticket findById(Long id);

    @Select("<script>" +
            "SELECT COUNT(*) FROM ticket" +
            "<where>" +
            "  <if test='title != null and title != \"\"'> AND title LIKE CONCAT('%',#{title},'%')</if>" +
            "  <if test='category != null and category != \"\"'> AND category = #{category}</if>" +
            "  <if test='priority != null and priority != \"\"'> AND priority = #{priority}</if>" +
            "  <if test='ticketStatus != null and ticketStatus != \"\"'> AND ticket_status = #{ticketStatus}</if>" +
            "</where>" +
            "</script>")
    long countByCondition(@Param("title") String title,
                          @Param("category") String category,
                          @Param("priority") String priority,
                          @Param("ticketStatus") String ticketStatus);

    @Select("<script>" +
            "SELECT * FROM ticket" +
            "<where>" +
            "  <if test='title != null and title != \"\"'> AND title LIKE CONCAT('%',#{title},'%')</if>" +
            "  <if test='category != null and category != \"\"'> AND category = #{category}</if>" +
            "  <if test='priority != null and priority != \"\"'> AND priority = #{priority}</if>" +
            "  <if test='ticketStatus != null and ticketStatus != \"\"'> AND ticket_status = #{ticketStatus}</if>" +
            "</where>" +
            " ORDER BY create_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Ticket> findByPage(@Param("title") String title,
                            @Param("category") String category,
                            @Param("priority") String priority,
                            @Param("ticketStatus") String ticketStatus,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    @Insert("INSERT INTO ticket (ticket_no, title, content, customer_name, category, priority, ticket_status, " +
            "ai_summary, ai_reply, ai_suggestion, assignee) " +
            "VALUES (#{ticketNo}, #{title}, #{content}, #{customerName}, #{category}, #{priority}, #{ticketStatus}, " +
            "#{aiSummary}, #{aiReply}, #{aiSuggestion}, #{assignee})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Ticket ticket);

    @Update("<script>" +
            "UPDATE ticket <set>" +
            "  <if test='title != null'> title = #{title}, </if>" +
            "  <if test='content != null'> content = #{content}, </if>" +
            "  <if test='customerName != null'> customer_name = #{customerName}, </if>" +
            "  <if test='category != null'> category = #{category}, </if>" +
            "  <if test='priority != null'> priority = #{priority}, </if>" +
            "  <if test='ticketStatus != null'> ticket_status = #{ticketStatus}, </if>" +
            "  <if test='aiSummary != null'> ai_summary = #{aiSummary}, </if>" +
            "  <if test='aiReply != null'> ai_reply = #{aiReply}, </if>" +
            "  <if test='aiSuggestion != null'> ai_suggestion = #{aiSuggestion}, </if>" +
            "  <if test='assignee != null'> assignee = #{assignee}, </if>" +
            "</set> WHERE id = #{id}" +
            "</script>")
    int update(Ticket ticket);

    @Select("SELECT * FROM ticket WHERE title LIKE CONCAT('%',#{keyword},'%') " +
            "OR customer_name LIKE CONCAT('%',#{keyword},'%') " +
            "OR content LIKE CONCAT('%',#{keyword},'%') " +
            "OR ticket_no LIKE CONCAT('%',#{keyword},'%') " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<Ticket> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    @Delete("DELETE FROM ticket WHERE id = #{id}")
    int deleteById(Long id);
}
