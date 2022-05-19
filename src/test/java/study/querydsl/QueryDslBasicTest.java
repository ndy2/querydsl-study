package study.querydsl;


import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
class QueryDslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory query;

    @BeforeEach
    void setUp() {
        query = new JPAQueryFactory(em);

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
    void startJpql() {
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "짱구")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("짱구");
    }

    @Test
    void startQueryDslV1() {
        QMember m = new QMember("m");

        Member findMember = query
                .select(m)
                .from(m)
                .where(m.username.eq("짱구"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("짱구");
    }

    @Test
    void startQueryDslV2() {
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("짱구"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("짱구");
    }

    @Test
    void search() {
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("짱구")
                        .and(member.age.eq(5)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("짱구");
        assertThat(findMember.getAge()).isEqualTo(5);
    }

    @Test
    void searchAndParam() {
        Member findMember = query
                .selectFrom(member)
                .where(
                        member.username.eq("짱구"),
                        member.age.eq(5)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("짱구");
        assertThat(findMember.getAge()).isEqualTo(5);
    }

    @Test
    void resultFetch() {
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();

        Member fetchOne = query
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = query
                .selectFrom(member)
                .fetchFirst();
    }

    @Test
    void count() {
        // count 가 필요하면 별도로 쿼리를 날리자
        QueryResults<Member> results = query
                .selectFrom(member)
                .fetchResults();

        long count = results.getTotal();
        List<Member> content = results.getResults();

        // 이것도 deprecated
        long fetchCount = query
                .selectFrom(member)
                .fetchCount();

        //이렇게 쓰자
        Long totalCount = query
                //.select(Wildcard.count) //select count(*)
                .select(member.count()) //select count(member.id)
                .from(member)
                .fetchOne();
    }

    @Test
    void sort() {
        //이름 올림 차순 이름이 없으면 마지막 (nulls last)
        em.persist(new Member(null, 100));
        em.persist(new Member("훈이", 100));
        em.persist(new Member("철수", 100));

        List<Member> fetch = query
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member chulsoo = fetch.get(0);
        Member hoonee = fetch.get(1);
        Member nullmember = fetch.get(2);

        assertThat(chulsoo.getUsername()).isEqualTo("철수");
        assertThat(hoonee.getUsername()).isEqualTo("훈이");
        assertThat(nullmember.getUsername()).isNull();
    }

    @Test
    void paging() {
        List<Member> fetch = query
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(fetch).hasSize(2);
    }

    @Test
    void aggregation() {
        List<Tuple> result = query
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(26);
        assertThat(tuple.get(member.age.avg())).isEqualTo(6.5);
        assertThat(tuple.get(member.age.max())).isEqualTo(8);
        assertThat(tuple.get(member.age.min())).isEqualTo(5);

    }

    /**
     * 팀의 이름과 각 팀의 나이 함
     */
    @Test
    void groupBy() {
        List<Tuple> result = query
                .select(team.name, member.age.sum())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .orderBy(team.name.desc())
                .fetch();

        Tuple tupleA = result.get(0);
        Tuple tupleB = result.get(1);

        assertThat(tupleA.get(team.name)).isEqualTo("해바라기반");
        assertThat(tupleA.get(member.age.sum())).isEqualTo(11);
        assertThat(tupleB.get(team.name)).isEqualTo("장미반");
        assertThat(tupleB.get(member.age.sum())).isEqualTo(15);

    }
}
