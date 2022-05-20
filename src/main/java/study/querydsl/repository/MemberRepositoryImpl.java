package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import java.util.List;
import java.util.function.Supplier;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;


@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory query;

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition cond) {
        return query
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(cond.getUsername()),
                        teamNameEq(cond.getTeamName()),
                        ageBetween(cond.getAgeLoe(), cond.getAgeGoe()))
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition cond, Pageable pageable) {
        QueryResults<MemberTeamDto> results = query
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(cond.getUsername()),
                        teamNameEq(cond.getTeamName()),
                        ageBetween(cond.getAgeLoe(), cond.getAgeGoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        return new PageImpl<>(
                results.getResults(),
                pageable,
                results.getTotal()
        );
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition cond,
                Pageable pageable) {
            List<MemberTeamDto> content = query
                    .select(new QMemberTeamDto(
                            member.id.as("memberId"),
                            member.username,
                            member.age,
                            team.id.as("teamId"),
                            team.name.as("teamName")))
                    .from(member)
                    .leftJoin(member.team, team)
                    .where(
                            usernameEq(cond.getUsername()),
                            teamNameEq(cond.getTeamName()),
                            ageGoe(cond.getAgeGoe()),
                            ageLoe(cond.getAgeLoe())
                    )
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();
            
            JPAQuery<Long> countQuery = query
                    .select(member.count())
                    .from(member)
                    .leftJoin(member.team, team)
                    .where(
                            usernameEq(cond.getUsername()),
                            teamNameEq(cond.getTeamName()),
                            ageGoe(cond.getAgeGoe()),
                            ageLoe(cond.getAgeLoe())
                    );
            
            return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
        }

    private BooleanBuilder ageGoe(Integer ageGoe) {
        return nullSafeBuilder(() -> member.age.goe(ageGoe));
    }

    private BooleanBuilder ageLoe(Integer ageLoe) {
        return nullSafeBuilder(() -> member.age.loe(ageLoe));
    }

    private BooleanBuilder ageBetween(Integer ageLoe, Integer ageGoe) {
        return ageGoe(ageGoe).and(ageLoe(ageLoe));
    }

    private BooleanBuilder teamNameEq(String teamName) {
        return nullSafeBuilder(() -> team.name.eq(teamName));
    }

    private BooleanBuilder usernameEq(String username) {
        return nullSafeBuilder(() -> member.username.eq(username));
    }

    public static BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> f) {
        try {
            return new BooleanBuilder(f.get());
        } catch (IllegalArgumentException | NullPointerException e) {
            return new BooleanBuilder();
        }
    }
}
