package com.ns.business.adpater.in.web;

import com.ns.business.adpater.in.web.dto.ModifyUserDataRequest;
import com.ns.business.adpater.in.web.dto.RegisterUserDataRequest;
import com.ns.business.adpater.out.JwtTokenProvider;
import com.ns.business.application.port.in.FindUserDataUseCase;
import com.ns.business.application.port.in.ModifyUserDataUseCase;
import com.ns.business.application.port.in.RegisterUserDataUseCase;
import com.ns.business.application.port.in.command.FindUserDataCommand;
import com.ns.business.application.port.in.command.ModifyUserDataCommand;
import com.ns.business.application.port.in.command.RegisterUserDataCommand;
import com.ns.business.domain.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path="/data")
public class UserDataController {

    private final RegisterUserDataUseCase registerUserDataUseCase;
    private final ModifyUserDataUseCase modifyUserDataUseCase;
    private final FindUserDataUseCase findUserDataUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping(path = "/register")
    ResponseEntity<UserData> registerUserData(@RequestBody RegisterUserDataRequest request){
        Long memberId = jwtTokenProvider.getMembershipIdbyToken();

        if(memberId != request.getUserId())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        RegisterUserDataCommand command = createRegisterUserDataCommand(request);
        UserData userData = registerUserDataUseCase.registerUserData(command);
        return ResponseEntity.ok(userData);
    }

    @PostMapping(path="/update")
    ResponseEntity<UserData> modifyUserDataByUserId(@RequestBody ModifyUserDataRequest request){
        Long memberId = jwtTokenProvider.getMembershipIdbyToken();

        if(memberId != request.getUserId())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ModifyUserDataCommand command = createModifyUserDataCommand(request);
        UserData userData = modifyUserDataUseCase.modifyUserData(command);
        return ResponseEntity.ok(userData);
    }

    @GetMapping(path="/{userId}")
    ResponseEntity<UserData> findUserDataByUserId(@PathVariable Long userId){

        FindUserDataCommand command = FindUserDataCommand.builder()
                .userId(userId)
                .build();

        UserData userData = findUserDataUseCase.findUserData(command);
        return ResponseEntity.ok(userData);
    }

    private RegisterUserDataCommand createRegisterUserDataCommand(RegisterUserDataRequest request){
        return RegisterUserDataCommand.builder()
                .userId(request.getUserId())
                .name(request.getName())
                .gold(request.getGold())
                .highscore(request.getHighscore())
                .energy(request.getEnergy())
                .scenario(request.getScenario())
                .head(request.getHead())
                .body(request.getBody())
                .arm(request.getArm())
                .health(request.getHealth())
                .attack(request.getAttack())
                .critical(request.getCritical())
                .durability(request.getDurability())
                .build();
    }

    private ModifyUserDataCommand createModifyUserDataCommand(ModifyUserDataRequest request){
        return ModifyUserDataCommand.builder()
                .userId(request.getUserId())
                .gold(request.getGold())
                .highscore(request.getHighscore())
                .energy(request.getEnergy())
                .scenario(request.getScenario())
                .head(request.getHead())
                .body(request.getBody())
                .arm(request.getArm())
                .health(request.getHealth())
                .attack(request.getAttack())
                .critical(request.getCritical())
                .durability(request.getDurability())
                .build();
    }
}
