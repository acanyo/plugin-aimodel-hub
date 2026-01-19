package com.xhhao.aimodelhub.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * 聊天消息
 *
 * @author Handsome
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息角色
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建系统消息
     */
    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM.getValue(), content);
    }

    /**
     * 创建用户消息
     */
    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER.getValue(), content);
    }

    /**
     * 创建助手消息
     */
    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT.getValue(), content);
    }

    /**
     * 创建指定角色的消息
     */
    public static ChatMessage of(Role role, String content) {
        return new ChatMessage(role.getValue(), content);
    }

    /**
     * 消息角色枚举
     */
    @Getter
    @RequiredArgsConstructor
    public enum Role {
        /**
         * 系统消息
         */
        SYSTEM("system"),
        /**
         * 用户消息
         */
        USER("user"),
        /**
         * 助手消息
         */
        ASSISTANT("assistant");

        private final String value;
    }
}
