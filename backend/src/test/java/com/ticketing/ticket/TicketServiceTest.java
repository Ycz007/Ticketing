package com.ticketing.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.attachment.service.AttachmentService;
import com.ticketing.common.api.BusinessException;
import com.ticketing.notification.service.NotificationService;
import com.ticketing.ticket.domain.TicketCategory;
import com.ticketing.ticket.dto.TicketRequests.*;
import com.ticketing.ticket.mapper.TicketMapper;
import com.ticketing.ticket.model.*;
import com.ticketing.ticket.service.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import static com.ticketing.support.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TicketServiceTest {
    TicketMapper mapper; AttachmentService attachments; NotificationService notifications;
    TicketService service; TicketRow row;
    @BeforeEach void setup() {
        mapper=mock(TicketMapper.class); attachments=mock(AttachmentService.class); notifications=mock(NotificationService.class);
        service=new TicketService(mapper,new TicketPolicy(),attachments,notifications,new ObjectMapper(),transactions());
        row=ticket();
        when(mapper.lock(10L)).thenAnswer(inv -> row); when(mapper.find(10L)).thenAnswer(inv -> row);
        when(mapper.update(any())).thenReturn(1);
        doAnswer(inv -> { ((EventRow)inv.getArgument(0)).id=101L; return 1; }).when(mapper).addEvent(any());
    }
    @Test void currentSupportStartsWork() {
        service.start(user(2,"ticket:read:assigned","ticket:start"),10L,new Version(0L));
        assertEquals("IN_PROGRESS",row.status); assertEquals(1L,row.version);
        verify(mapper).addEvent(argThat(e -> e.eventType.equals("STARTED")));
    }
    @Test void assignedCoordinatorCanProcessConsultation() {
        row.assigneeId=3L; row.category="USAGE_QUESTION"; row.status="IN_PROGRESS";
        service.result(user(3,"ticket:read:all","ticket:result:submit"),10L,new Result(0L,"ANSWERED","请从个人设置修改密码"));
        assertEquals("AWAITING_CONFIRMATION",row.status); assertEquals("ANSWERED",row.resolutionCode);
    }
    @Test void unassignedCoordinatorCannotSubmitResult() {
        row.status="IN_PROGRESS";
        assertThrows(AccessDeniedException.class,() -> service.result(user(3,"ticket:read:all","ticket:result:submit"),10L,new Result(0L,"RESOLVED","处理完成")));
        verify(mapper,never()).update(any());
    }
    @Test void featureSuggestionCanBeRejectedWithReason() {
        row.category="FEATURE_SUGGESTION"; row.status="IN_PROGRESS";
        service.result(user(2,"ticket:read:assigned","ticket:result:submit"),10L,new Result(0L,"REJECTED","已有替代功能，暂不开发"));
        assertEquals("AWAITING_CONFIRMATION",row.status);
        verify(notifications).publish(eq(10L),eq(101L),eq("RESULT_SUBMITTED"),eq(2L),eq(List.of(1L)));
    }
    @Test void consultationCannotUsePermissionApprovalResult() {
        row.category="USAGE_QUESTION"; row.status="IN_PROGRESS";
        assertThrows(BusinessException.class,() -> service.result(user(2,"ticket:read:assigned","ticket:result:submit"),10L,new Result(0L,"APPROVED","审批通过")));
        verify(mapper,never()).update(any());
    }
    @Test void onlyRequesterConfirmsCompletion() {
        row.status="AWAITING_CONFIRMATION";
        service.confirm(user(1,"ticket:read:own","ticket:confirm"),10L,new Version(0L));
        assertEquals("COMPLETED",row.status); assertNotNull(row.completedAt);
    }
    @Test void coordinatorCannotConfirmOnBehalfOfRequester() {
        row.status="AWAITING_CONFIRMATION";
        assertThrows(AccessDeniedException.class,() -> service.confirm(user(3,"ticket:read:all"),10L,new Version(0L)));
    }
    @Test void requesterRejectsResultAndKeepsHandler() {
        row.status="AWAITING_CONFIRMATION"; row.resolutionCode="RESOLVED";
        service.rework(user(1,"ticket:read:own","ticket:rework"),10L,new Rework(0L,"导出仍然无文件"));
        assertEquals("REWORK",row.status); assertEquals(2L,row.assigneeId); assertNull(row.resolutionCode);
    }
    @Test void whitespaceReworkReasonIsRejected() {
        row.status="AWAITING_CONFIRMATION";
        assertThrows(BusinessException.class,() -> service.rework(user(1,"ticket:read:own","ticket:rework"),10L,new Rework(0L," \t\n　")));
    }
    @Test void transferPreservesStatusAndInvalidatesOldSupportNotifications() {
        row.status="IN_PROGRESS";
        when(mapper.handler(3L)).thenReturn(new HandlerOption(3L,"协调员","COORDINATOR"));
        when(mapper.handler(2L)).thenReturn(new HandlerOption(2L,"支持","SUPPORT"));
        service.assign(user(4,"ticket:read:all","ticket:assign","ticket:reassign"),10L,new Assign(0L,3L,"需要业务审批跟进"));
        assertEquals("IN_PROGRESS",row.status); assertEquals(3L,row.assigneeId);
        verify(notifications).invalidate(10L,2L);
    }
    @Test void changingCategoryDoesNotOverwriteOriginalChoice() {
        service.category(user(3,"ticket:read:all","ticket:category:update"),10L,new Category(0L,TicketCategory.USAGE_QUESTION,"实际是操作咨询"));
        assertEquals("SYSTEM_FAILURE",row.initialCategory); assertEquals("USAGE_QUESTION",row.category);
    }
    @Test void createKeepsRawTextAndRetryDoesNotCreateAnotherEvent() {
        var request=new Create("R100","  我想增加报表\n",TicketCategory.FEATURE_SUGGESTION,null,List.of());
        var captured=new java.util.concurrent.atomic.AtomicReference<TicketRow>();
        when(mapper.findRequest("R100")).thenAnswer(inv -> captured.get());
        when(mapper.insert(any())).thenAnswer(inv -> { var created=(TicketRow)inv.getArgument(0); created.id=10L; captured.set(created); return 1; });
        when(mapper.find(10L)).thenAnswer(inv -> captured.get());
        var requester=user(1,"ticket:create");
        var first=service.create(requester,request); var retry=service.create(requester,request);
        assertEquals(first.id,retry.id); assertEquals(request.originalText(),first.originalText);
        assertEquals("FEATURE_SUGGESTION",first.category); verify(mapper,times(1)).insert(any());
        verify(mapper,times(1)).addEvent(any());
    }
    @Test void duplicateRequestIdFromAnotherPersonDoesNotExposeTicket() {
        when(mapper.findRequest("R001")).thenReturn(row);
        assertEquals(409,assertThrows(BusinessException.class,() -> service.create(user(9,"ticket:create"),
                new Create("R001","消息",TicketCategory.UNCONFIRMED,null,List.of()))).status());
    }
    @Test void concurrentDuplicateInsertIsHandledAfterRollback() {
        var req=new Create("R001","原文",TicketCategory.UNCONFIRMED,null,List.of());
        var digest=AttachmentService.hash("[\"原文\",\"UNCONFIRMED\",null,[]]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        row.creationDigest=digest;
        when(mapper.findRequest("R001")).thenReturn(null,row);
        when(mapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));
        assertEquals(10L,service.create(user(1,"ticket:create"),req).id);
        verify(mapper,never()).addEvent(any());
    }
    @Test void blankFeedbackIsRejectedBeforeInsert() {
        assertThrows(BusinessException.class,() -> service.create(user(1,"ticket:create"),new Create("R001","　\n",TicketCategory.UNCONFIRMED,null,List.of())));
        verify(mapper,never()).insert(any());
    }
    @Test void supportCannotReadAnotherHandlersDetail() {
        assertEquals(404,assertThrows(BusinessException.class,() -> service.detail(user(8,"ticket:read:assigned"),10L)).status());
        verify(mapper,never()).events(any());
    }

    private void accessRequest() {
        row.category="ACCESS_REQUEST"; row.assigneeId=null;
        var application=new UserPermissionRow(); application.ticketId=10L; application.status="PENDING"; application.permissionCode="ticket:read:all";
        when(mapper.permissionRequest(10L)).thenReturn(application);
        when(mapper.permission("ticket:read:all")).thenReturn(new PermissionOption("ticket:read:all","读取全部工单"));
        when(mapper.reviewPermissionRequest(anyLong(),anyString(),anyLong(),nullable(String.class))).thenReturn(1);
    }
    @Test void coordinatorApprovesWithoutAssignmentOrRequesterConfirmation() {
        accessRequest();
        service.approve(user(3,"ROLE_COORDINATOR","ticket:read:all","ticket:access:approve"),10L,new Result(0L,"APPROVED","同意开通"));
        assertEquals("COMPLETED",row.status); assertEquals("APPROVED",row.resolutionCode);
        assertEquals(3L,row.assigneeId); assertNotNull(row.completedAt);
        verify(mapper).reviewPermissionRequest(10L,"APPROVED",3L,"ticket:read:all");
        verify(notifications).publish(10L,101L,"TICKET_COMPLETED",3L,List.of(1L));
    }
    @Test void coordinatorRejectionEndsApplicationWithoutGrant() {
        accessRequest();
        service.approve(user(3,"ROLE_COORDINATOR","ticket:read:all","ticket:access:approve"),10L,new Result(0L,"REJECTED","不符合岗位职责"));
        assertEquals("COMPLETED",row.status); assertEquals("REJECTED",row.resolutionCode);
        verify(mapper).reviewPermissionRequest(10L,"REJECTED",3L,"ticket:read:all");
    }
    @Test void requesterAndSupportCannotApproveEvenWithApprovalAuthority() {
        accessRequest();
        for (String role:List.of("ROLE_REQUESTER","ROLE_SUPPORT")) {
            assertThrows(AccessDeniedException.class,() -> service.approve(user(1,role,"ticket:access:approve","ticket:read:all"),10L,new Result(0L,"APPROVED","同意")));
        }
        verify(mapper,never()).reviewPermissionRequest(anyLong(),anyString(),anyLong(),nullable(String.class));
    }
    @Test void duplicateApprovalAndStaleVersionCannotChangeGrant() {
        accessRequest();
        var coordinator=user(3,"ROLE_COORDINATOR","ticket:read:all","ticket:access:approve");
        assertThrows(BusinessException.class,() -> service.approve(coordinator,10L,new Result(1L,"APPROVED","同意")));
        row.status="COMPLETED";
        assertThrows(BusinessException.class,() -> service.approve(coordinator,10L,new Result(0L,"APPROVED","同意")));
        verify(mapper,never()).reviewPermissionRequest(anyLong(),anyString(),anyLong(),nullable(String.class));
    }
    @Test void accessRequestCannotBypassApprovalThroughNormalResult() {
        accessRequest(); row.assigneeId=2L; row.status="IN_PROGRESS";
        assertThrows(BusinessException.class,() -> service.result(user(2,"ticket:read:assigned","ticket:result:submit"),10L,new Result(0L,"APPROVED","同意")));
        verify(mapper,never()).update(any());
    }
    @Test void accessApplicationRequiresPermissionNameAndRequesterRole() {
        assertThrows(BusinessException.class,() -> service.create(user(1,"ROLE_REQUESTER","ticket:create"),new Create("A1","申请原因",TicketCategory.ACCESS_REQUEST,null,List.of(),"  ")));
        assertThrows(AccessDeniedException.class,() -> service.create(user(2,"ROLE_SUPPORT","ticket:create"),new Create("A1","申请原因",TicketCategory.ACCESS_REQUEST,null,List.of(),"ticket:read:all")));
        verify(mapper,never()).insert(any());
    }
    @Test void accessCreationStoresApplicationAndRetryIncludesPermissionName() {
        when(mapper.permission("ticket:read:all")).thenReturn(new PermissionOption("ticket:read:all","读取全部工单"));
        when(mapper.permission("ticket:read:assigned")).thenReturn(new PermissionOption("ticket:read:assigned","读取负责工单"));
        when(mapper.insertPermissionRequest(anyLong(),anyLong(),anyString())).thenReturn(1);
        var captured=new java.util.concurrent.atomic.AtomicReference<TicketRow>();
        when(mapper.findRequest("A1")).thenAnswer(inv -> captured.get());
        when(mapper.insert(any())).thenAnswer(inv -> { var created=(TicketRow)inv.getArgument(0); created.id=10L; captured.set(created); return 1; });
        when(mapper.find(10L)).thenAnswer(inv -> captured.get());
        var requester=user(1,"ROLE_REQUESTER","ticket:create");
        var request=new Create("A1","申请原因",TicketCategory.ACCESS_REQUEST,null,List.of(),"ticket:read:all");
        service.create(requester,request); service.create(requester,request);
        verify(mapper,times(1)).insertPermissionRequest(10L,1L,"ticket:read:all");
        assertThrows(BusinessException.class,() -> service.create(requester,new Create("A1","申请原因",TicketCategory.ACCESS_REQUEST,null,List.of(),"ticket:read:assigned")));
    }

    @Test void approvalActionReplacesAssignmentAndOrdinaryResultActions() {
        accessRequest(); row.status="IN_PROGRESS"; row.assigneeId=3L;
        var actions=service.detail(user(3,"ROLE_COORDINATOR","ticket:read:all","ticket:access:approve","ticket:assign","ticket:result:submit"),10L).actions();
        assertEquals(java.util.Set.of("approve"),actions);
    }
    @Test void ordinaryTicketAndUnsupportedDecisionCannotBeApproved() {
        var coordinator=user(3,"ROLE_COORDINATOR","ticket:read:all","ticket:access:approve");
        assertThrows(BusinessException.class,() -> service.approve(coordinator,10L,new Result(0L,"APPROVED","同意")));
        accessRequest();
        assertThrows(BusinessException.class,() -> service.approve(coordinator,10L,new Result(0L,"RESOLVED","已解决")));
        assertThrows(BusinessException.class,() -> service.approve(coordinator,10L,new Result(0L,"APPROVED","　")));
        verify(mapper,never()).reviewPermissionRequest(anyLong(),anyString(),anyLong(),nullable(String.class));
    }

    @Test void unknownPermissionCannotBeRequestedOrGranted() {
        assertThrows(BusinessException.class,() -> service.create(user(1,"ROLE_REQUESTER","ticket:create"),new Create("A2","申请原因",TicketCategory.ACCESS_REQUEST,null,List.of(),"not:a:permission")));
        accessRequest();
        var coordinator=user(3,"ROLE_COORDINATOR","ticket:read:all","ticket:access:approve");
        assertThrows(BusinessException.class,() -> service.approve(coordinator,10L,new Result(0L,"APPROVED","同意","not:a:permission")));
        verify(mapper,never()).reviewPermissionRequest(anyLong(),anyString(),anyLong(),nullable(String.class));
    }
    @Test void legacyRequestRequiresExplicitPermissionBeforeGrant() {
        accessRequest();
        var legacy=new UserPermissionRow(); legacy.status="PENDING";
        when(mapper.permissionRequest(10L)).thenReturn(legacy);
        var coordinator=user(3,"ROLE_COORDINATOR","ticket:read:all","ticket:access:approve");
        assertThrows(BusinessException.class,() -> service.approve(coordinator,10L,new Result(0L,"APPROVED","同意")));
        service.approve(coordinator,10L,new Result(0L,"APPROVED","同意","ticket:read:all"));
        verify(mapper).reviewPermissionRequest(10L,"APPROVED",3L,"ticket:read:all");
    }
}
