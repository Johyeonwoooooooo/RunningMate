package RunningMate.backend.domain.running.service;

import RunningMate.backend.domain.running.entity.*;
import RunningMate.backend.domain.running.entity.Record;
import RunningMate.backend.domain.user.entity.User;
import RunningMate.backend.domain.running.dto.RunningDTO;
import RunningMate.backend.domain.running.repository.LeaderBoardRepository;
import RunningMate.backend.domain.running.repository.RecordRepository;
import RunningMate.backend.domain.running.repository.RunningGroupRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunningServiceImpl implements RunningService {
    private final RunningGroupRepository groupRepository;
    private final LeaderBoardRepository leaderBoardRepository;
    private final RecordRepository recordRepository;
    @Override
    public RunningGroup makeRunningGroup(RunningDTO.MakeRunningGroupRequest request, Optional<User> optionalUser) {
        if(optionalUser.isEmpty())
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");

        if(request.getMaxParticipants().equals(0))
            throw new IllegalArgumentException("최대 참가자는 1명 이상 이어야 합니다.");

        return groupRepository.save(RunningGroup.builder().groupTitle(request.getGroupTitle())
                .groupTag(request.getGroupTag())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .currentParticipants(0)
                .maxParticipants(request.getMaxParticipants())
                .targetDistance(request.getTargetDistance())
                .activate(true)
                .build());
    }

    @Override
    public RunningDTO.ParticipateGroupResponse participateGroup(Long groupId, Optional<User> optionalUser) {
        if(optionalUser.isEmpty())
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");

        RunningGroup group = groupRepository.findByGroupId(groupId);
        if(group == null)
            throw new IllegalArgumentException("해당 러닝방을 찾을 수 없습니다.");

        if(!group.participateGroup())
            throw new IllegalArgumentException("최대 참가자를 달성하여 참가할 수 없습니다.");
        groupRepository.save(group);
        Record record = recordRepository.save(Record.builder().user(optionalUser.get())
                .runningStartTime(LocalDate.now()).runningTime(Duration.ZERO).calories(0L).distance(0L).build());
        LeaderBoard leaderBoard = LeaderBoard.builder().group(group).record(record).ranking(0L).build();
        leaderBoardRepository.save(leaderBoard);

        return new RunningDTO.ParticipateGroupResponse(record);
    }

    @Override
    public List<RunningDTO.RunningGroupViewResponse> viewRunningGroups() {
        List<RunningGroup> groupList = groupRepository.findAllByStartTimeAfter(LocalDateTime.now());
        return groupList.stream().map(RunningDTO.RunningGroupViewResponse::new).toList();
    }

    @Override
    public List<RunningDTO.RunningGroupViewResponse> filteringGroup(GroupTag groupTag, String searchWord) {
        List<RunningGroup> groupList;
        if(groupTag == null){
            groupList = groupRepository.findAllByGroupTitleContainsAndStartTimeAfter(searchWord, LocalDateTime.now());
        }
        else {
            groupList = groupRepository.findAllByGroupTagAndGroupTitleContainsAndStartTimeAfter(groupTag, searchWord, LocalDateTime.now());
        }

        return groupList.stream().map(RunningDTO.RunningGroupViewResponse::new).toList();
    }

    @Override
    public RunningDTO.groupParticipantResponse groupParticipants(Long groupId) {
        RunningGroup group = groupRepository.findByGroupId(groupId);
        if(group == null)
            throw new IllegalArgumentException("해당 러닝방이 존재하지 않습니다.");

        List<LeaderBoard> groups = leaderBoardRepository.findAllByGroup(group);
        List<String> participants = new ArrayList<>();
        for (LeaderBoard leaderBoard : groups) {
            participants.add(leaderBoard.getRecord().getUser().getUserNickname());
        }

        return RunningDTO.groupParticipantResponse.builder().groupTitle(group.getGroupTitle())
                .groupTag(group.getGroupTag()).endTime(group.getEndTime()).startTime(group.getStartTime())
                .currentParticipants(group.getCurrentParticipants()).maxParticipants(group.getMaxParticipants())
                .participants(participants).targetDistance(group.getTargetDistance()).build();
    }

    @Scheduled(cron="30 59 18 * * *")
    @Override
    public void autoCreateQuickRunningGroup() {
//        RunningGroup group = groupRepository.findByGroupTagAndActivate(GroupTag.QUICK, true);
//        group.deactivate();
//        groupRepository.save(group);

        groupRepository.save(RunningGroup.builder()
                            .groupTitle("빠른 매칭방")
                            .groupTag(GroupTag.QUICK)
                            .startTime(LocalDateTime.now())
                            .endTime(LocalDateTime.now().plusDays(1))
                            .currentParticipants(0)
                            .maxParticipants(Integer.MAX_VALUE)
                            .targetDistance(Long.MAX_VALUE)
                             .activate(true)
                            .build());
    }

    @Override
    public RunningDTO.ParticipateQuickRunningResponse participateQuickRunning(Optional<User> optionalUser) {
        if(optionalUser.isEmpty())
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");

        RunningGroup group = groupRepository.findByGroupTagAndActivate(GroupTag.QUICK, true);

        Record record = recordRepository.save(Record.builder().user(optionalUser.get())
                .runningStartTime(LocalDate.now()).runningTime(Duration.ZERO).calories(0L).distance(0L).build());
        LeaderBoard leaderBoard = LeaderBoard.builder().group(group).record(record).ranking(0L).build();
        leaderBoardRepository.save(leaderBoard);

        return new RunningDTO.ParticipateQuickRunningResponse(record);
    }

    @Override
    @Transactional
    public void cancelParticipation(RunningDTO.CancelParticipationRequest request) {
        RunningGroup group = groupRepository.findByGroupId(request.getGroupId());
        if(group == null)
            throw new IllegalArgumentException("해당 러닝방이 존재하지 않습니다.");
        Record record = recordRepository.findRecordByRecordId(request.getRecordId());
        if(group == null)
            throw new IllegalArgumentException("해당 기록이 존재하지 않습니다.");

        if(!group.leaveGroup())
            throw new IllegalArgumentException("이미 참가자가 없습니다.");
        leaderBoardRepository.deleteLeaderBoardByGroupAndRecord(group, record);
        recordRepository.deleteRecordByRecordId(request.getRecordId());
    }

    @Override
    @Scheduled(fixedRate = 5000) // 5초마다 반복. 60000 = 1분
    public void deactivateRunningGroup() {
        List<RunningGroup> runningGroups = groupRepository.findAllByEndTimeBefore(LocalDateTime.now());
        for (RunningGroup runningGroup : runningGroups) {
            runningGroup.deactivate();
            groupRepository.save(runningGroup);
        }
    }

//    @Override
//    @Scheduled(fixedRate = 1000)
//    @Transactional
//    public void autoDeleteRunningGroup() {
//        groupRepository.deleteAllByEndTimeBefore(LocalDateTime.now().plusDays(1));
//    }

    @Override
    public List<RunningDTO.MainPageGroupResponse> mainPageGroups() {
        List<RunningGroup> groupList = groupRepository.findAllByStartTimeAfter(LocalDateTime.now());
        return groupList.stream().limit(6).map(RunningDTO.MainPageGroupResponse::new).toList();
    }
}
