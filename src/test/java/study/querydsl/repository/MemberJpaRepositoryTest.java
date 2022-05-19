package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class MemberJpaRepositoryTest {


    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository repository;

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


    @Test
    void basicTest_queryDsl() {
        Member member = new Member("짱구", 5);

        repository.save(member);

        Member findMember = repository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> all = repository.findAll_queryDsl();
        assertThat(all).containsExactly(member);

        List<Member> members = repository.findByUsername_queryDsl("짱구");
        assertThat(members).containsExactly(member);
    }

    @Test
    void searchTest_booleanBuilder() {
        initData();

        MemberSearchCondition cond = new MemberSearchCondition();
        cond.setTeamName("해바라기반");
        cond.setAgeLoe(10);

        List<MemberTeamDto> result = repository.searchByBuilder(cond);

        assertThat(result).extracting(MemberTeamDto::getUsername).containsExactly("짱구", "유리");
        assertThat(result).extracting(MemberTeamDto::getAge).containsExactly(5, 6);
        assertThat(result).extracting(MemberTeamDto::getTeamName).containsExactly("해바라기반", "해바라기반");
    }

    @Test
    void searchTest_다중_where() {
        initData();

        MemberSearchCondition cond = new MemberSearchCondition();
        cond.setTeamName("해바라기반");
        cond.setAgeLoe(10);

        List<MemberTeamDto> result = repository.search(cond);

        assertThat(result).extracting(MemberTeamDto::getUsername).containsExactly("짱구", "유리");
        assertThat(result).extracting(MemberTeamDto::getAge).containsExactly(5, 6);
        assertThat(result).extracting(MemberTeamDto::getTeamName).containsExactly("해바라기반", "해바라기반");
    }



    void initData() {
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
}