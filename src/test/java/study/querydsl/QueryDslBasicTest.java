package study.querydsl;


import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;

import static com.querydsl.core.types.ExpressionUtils.as;
import static com.querydsl.jpa.JPAExpressions.select;
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

    /**
     * 조인
     */
    @Test
    void join() {
        List<Member> results = query
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("해바라기반"))
                .fetch();

        assertThat(results)
                .extracting(Member::getUsername)
                .containsExactly("짱구", "유리");
    }

    /**
     * Select m, t from Member m left join m.team t on t.name = `해바라기반`
     */
    @Test
    void join_on_filtering() {
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("해바라기반"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    /**
     * inner join -> on, where 똑같음
     */
    @Test
    void inner_join_on_혹은_where_filtering() {
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .join(member.team, team)
//                .on(team.name.eq("해바라기반"))
                .where(team.name.eq("해바라기반"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Autowired
    EntityManagerFactory emf;

    /**
     * no fetch join
     */
    @Test
    void no_fetch_join() {
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("짱구"))
                .fetchOne();

        assertThat(emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()))
                .as("페치 조인 미적용").isFalse();

        findMember.getTeam().getName();
        assertThat(emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()))
                .as("이제 로딩됨").isTrue();
    }

    /**
     * fetch join
     */
    @Test
    public void fetch_join() throws Exception {
        Member findMember = query
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("짱구"))
                .fetchOne();

        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    @Test
    void sub_query() {
        QMember sub = new QMember("sub");

        Member member = query
                .selectFrom(QMember.member)
                .where(QMember.member.age.eq(
                        select(sub.age.max())
                                .from(sub)
                ))
                .fetchOne();

        assertThat(member.getUsername()).isEqualTo("둘리");
    }

    @Test
    void sub_query_in() {
        QMember sub = new QMember("sub");

        List<Member> members = query
                .selectFrom(member)
                .where(member.age.in(
                        select(sub.age)
                                .from(sub)
                                .where(sub.age.lt(7))
                ))
                .fetch();

        assertThat(members)
                .extracting(Member::getUsername)
                .containsExactly("짱구", "유리");
    }

    @Test
    void select_절_sub_query() {
        QMember sub = new QMember("sub");

        List<Tuple> fetch = query
                .select(member.username,
                        select(sub.age.avg())
                                .from(sub)
                ).from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " +
                    tuple.get(select(sub.age.avg())
                            .from(sub)));
        }
    }

    @Test
    void case_문() {

        List<String> result = query
                .select(member.age.when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")).from(member)
                .fetch();
    }

    @Test
    void case_builder() {
        List<String> result = query
                .select(
                        new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0~20살")
                                .when(member.age.between(21, 30)).then("21~30살")
                                .otherwise("기타"))
                .from(member)
                .fetch();
    }

    @Test
    void numberExpr() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

         List<Tuple> result = query
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = "
                    + rank);
        }
    }

    @Test
    void 상수() {
        Tuple result = query
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetchFirst();
    }

    @Test
    void 문자열_더하기() {
        String result = query
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
    }

    @Test
    void simpleProjection() {
        List<String> result = query
                .select(member.username)
                .from(member)
                .fetch();

        assertThat(result).containsExactly("짱구", "유리", "치타", "둘리");
    }

    @Test
    void tupleProjection() {
        List<Tuple> result = query
                .select(member.username, member.age)
                .from(member)
                .fetch();

        assertThat(result).extracting(t -> t.get(member.username)).containsExactly("짱구", "유리", "치타", "둘리");
        assertThat(result).extracting(t -> t.get(member.age)).containsExactly(5, 6, 7, 8);
    }

    @Test
    void dtoProjectionJpql() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                        "from Member m", MemberDto.class)
                .getResultList();

        assertThat(result).extracting(MemberDto::getUsername).containsExactly("짱구", "유리", "치타", "둘리");
        assertThat(result).extracting(MemberDto::getAge).containsExactly(5, 6, 7, 8);
    }

    @Test
    void dtoProjectionQuerydsl_세터() {
        List<MemberDto> result = query
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        assertThat(result).extracting(MemberDto::getUsername).containsExactly("짱구", "유리", "치타", "둘리");
        assertThat(result).extracting(MemberDto::getAge).containsExactly(5, 6, 7, 8);
    }


    @Test
    void dtoProjectionQuerydsl_필드() {
        List<MemberDto> result = query
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        assertThat(result).extracting(MemberDto::getUsername).containsExactly("짱구", "유리", "치타", "둘리");
        assertThat(result).extracting(MemberDto::getAge).containsExactly(5, 6, 7, 8);
    }

    @Test
    void dtoProjectionQuerydsl_생성자() {
        List<MemberDto> result = query
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        assertThat(result).extracting(MemberDto::getUsername).containsExactly("짱구", "유리", "치타", "둘리");
        assertThat(result).extracting(MemberDto::getAge).containsExactly(5, 6, 7, 8);
    }


    @Test
    void userdto_ProjectionQuerydsl_생성자() {
        QMember sub = new QMember("sub");

        List<UserDto> result = query
                .select(Projections.constructor(UserDto.class,
                        member.username.as("name"),
                        as(
                                select(sub.age.max())
                                .from(sub), "age"))
                )
                .from(member)
                .fetch();

        assertThat(result).extracting(UserDto::getName).containsExactly("짱구", "유리", "치타", "둘리");
        assertThat(result).extracting(UserDto::getAge).containsExactly(8, 8, 8, 8);
    }
}
