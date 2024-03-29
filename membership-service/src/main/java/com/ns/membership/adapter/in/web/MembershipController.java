package com.ns.membership.adapter.in.web;

import com.ns.membership.adapter.in.web.dto.ModifyMembershipRequest;
import com.ns.membership.adapter.in.web.dto.RegisterMembershipRequest;
import com.ns.membership.application.port.in.FindMembershipUseCase;
import com.ns.membership.application.port.in.ModifyMembershipUseCase;
import com.ns.membership.application.port.in.RegisterMembershipUseCase;
import com.ns.membership.application.port.in.UserDataRequestUseCase;
import com.ns.membership.application.port.in.command.FindMembershipCommand;
import com.ns.membership.application.port.in.command.ModifyMembershipCommand;
import com.ns.membership.application.port.in.command.RegisterMembershipCommand;
import com.ns.membership.application.port.in.command.UserDataRequestCommand;
import com.ns.membership.domain.Membership;
import com.ns.membership.domain.userData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path="/membership")
public class MembershipController {


    private final RegisterMembershipUseCase registerMembershipUseCase;
    private final ModifyMembershipUseCase modifyMembershipUseCase;
    private final FindMembershipUseCase findMembershipUseCase;
    private final UserDataRequestUseCase userDataRequestUseCase;

    @PostMapping (path = "/register")
    Membership registerMembership(@RequestBody RegisterMembershipRequest request){
        // request -> Command로 추상화
        // UseCase ~~(request x, command)

        RegisterMembershipCommand command = RegisterMembershipCommand.builder()
                .name(request.getName())
                .address(request.getAddress())
                .email(request.getEmail())
                .isValid(true)
                .build();

        return registerMembershipUseCase.registerMembership(command);
    }

    @PostMapping(path="/modify/")
    ResponseEntity<Membership> modifyMembershipByMemberId(@RequestBody ModifyMembershipRequest request){

        ModifyMembershipCommand command = ModifyMembershipCommand.builder()
                .membershipId(request.getMembershipId())
                .name(request.getName())
                .address(request.getAddress())
                .email(request.getEmail())
                .isValid(request.isValid())
                .build();

        return ResponseEntity.ok(modifyMembershipUseCase.modifyMembership(command));
    }

    @GetMapping(path="/{membershipId}")
    ResponseEntity<Membership> findMembershipByMemberId(@PathVariable String membershipId){

        FindMembershipCommand command = FindMembershipCommand.builder()
                .membershipId(membershipId)
                .build();

        return ResponseEntity.ok(findMembershipUseCase.findMembership(command));
    }

    @GetMapping (path = "/data/{membershipId}")
    ResponseEntity<List<userData>> getUserData(@PathVariable String membershipId){

        Set<Long> targetIdSet = new HashSet<>(Collections.singletonList(Long.parseLong(membershipId)));

        UserDataRequestCommand command = UserDataRequestCommand.builder().
                membershipId("") //요청을 보낸 사용자의 id인데, 여기선 그냥 때려박았다.
                .targetIdList(targetIdSet).build();
        List<userData> detail = userDataRequestUseCase.getUserData(command);

        return ResponseEntity.ok(detail);
    }

    @GetMapping("/ally/random/{membershipId}")
    ResponseEntity<List<userData>> getAllyRandom(@PathVariable String membershipId){

        Set<Long> targetIdList = userDataRequestUseCase.getAllyRandom(membershipId);

        UserDataRequestCommand command = UserDataRequestCommand.builder().
                membershipId(membershipId)
                .targetIdList(targetIdList).build();

        log.info(targetIdList.toString());
        List<userData> detail = userDataRequestUseCase.getUserData(command);
        return ResponseEntity.ok(detail);
    }
}
