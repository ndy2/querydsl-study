package study.querydsl.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository repository;

    @BeforeEach
    void setUp() {
        Team teamA = new Team("해바라기반");
        Team teamB = new Team("장미반");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("짱구", 5, teamA);
        Member member2 = new Member("유리", 6, teamA);
        Member member3 = new Member("치타", 7, teamB);
        Member member4 = new Member("둘리", 8, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();

        System.out.println("=====================");
    }

    @Test
    void searchTest_다중_where() {
        MemberSearchCondition cond = new MemberSearchCondition();
        cond.setTeamName("해바라기반");
        cond.setAgeLoe(10);

        List<MemberTeamDto> result = repository.search(cond);

        assertThat(result).extracting(MemberTeamDto::getUsername).containsExactly("짱구", "유리");
        assertThat(result).extracting(MemberTeamDto::getAge).containsExactly(5, 6);
        assertThat(result).extracting(MemberTeamDto::getTeamName).containsExactly("해바라기반", "해바라기반");
    }


    @Test
    void searchPageTest_간단() {
        MemberSearchCondition cond = new MemberSearchCondition();

        Page<MemberTeamDto> page = repository.searchPageSimple(cond, Pageable.ofSize(3));
        List<MemberTeamDto> result = page.getContent();

        assertThat(result).extracting(MemberTeamDto::getUsername).containsExactly("짱구", "유리", "치타");
        assertThat(result).extracting(MemberTeamDto::getAge).containsExactly(5, 6, 7);
        assertThat(result).extracting(MemberTeamDto::getTeamName).containsExactly("해바라기반", "해바라기반", "장미반");
    }
}