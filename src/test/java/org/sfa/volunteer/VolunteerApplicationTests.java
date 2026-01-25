package org.sfa.volunteer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class VolunteerApplicationTests {

	@Test
	void main() {
		VolunteerApplication.main(new String[] {});
		assertThat(true).isTrue();
	}

}

