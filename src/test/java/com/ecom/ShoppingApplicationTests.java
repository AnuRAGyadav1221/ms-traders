package com.ecom;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestMailSenderConfig.class)
class ShoppingApplicationTests {

	@Test
	void contextLoads() {
	}

}
