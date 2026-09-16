package com.pet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 上下文加载冒烟测试:用 test profile(H2 内存库 + 测试密钥),
 * 验证 Spring 容器能正常启动,所有 Bean 装配无环、无缺配置。
 */
@SpringBootTest
@ActiveProfiles("test")
class PetApplicationTests {

    @Test
    void contextLoads() {
        // 仅验证上下文能加载,断言由 Spring 启动过程本身完成
    }
}
