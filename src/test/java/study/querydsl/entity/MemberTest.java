package study.querydsl.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

@SpringBootTest
@Transactional
@Commit
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    void testEntity() {
        Team teamA = new Team("해바라기반");
        Team teamB = new Team("장미반");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("짱구", 5, teamA);
        Member member2 = new Member("유리", 5, teamA);
        Member member3 = new Member("치타", 5, teamB);
        Member member4 = new Member("둘리", 5, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();

        //when
        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        for (Member member : members) {
            System.out.println();
            System.out.println("=========");
            System.out.println("member = " + member);
            System.out.println("-> member.getTeam() = " + member.getTeam());
        }
    }
}