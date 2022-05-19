package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    MemberRepository repository;

    @Test
    void basicTest() {
        Member member = new Member("짱구", 5);

        repository.save(member);

        Member findMember = repository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> all = repository.findAll();
        assertThat(all).containsExactly(member);

        List<Member> members = repository.findByUsername("짱구");
        assertThat(members).containsExactly(member);
    }
}