package com.ticketing.ticket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.attachment.service.AttachmentService;
import com.ticketing.auth.security.SessionUser;
import com.ticketing.common.api.*;
import com.ticketing.notification.service.NotificationService;
import com.ticketing.ticket.domain.*;
import com.ticketing.ticket.dto.TicketRequests.*;
import com.ticketing.ticket.mapper.TicketMapper;
import com.ticketing.ticket.model.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TicketService {
    private final TicketMapper mapper;
    private final TicketPolicy policy;
    private final AttachmentService attachments;
    private final NotificationService notifications;
    private final ObjectMapper json;
    private final TransactionTemplate tx;
    public TicketService(TicketMapper mapper, TicketPolicy policy, AttachmentService attachments,
            NotificationService notifications, ObjectMapper json, PlatformTransactionManager transactions) {
        this.mapper=mapper; this.policy=policy; this.attachments=attachments; this.notifications=notifications;
        this.json=json; this.tx=new TransactionTemplate(transactions);
    }

    public TicketRow create(SessionUser user, Create request) {
        policy.require(user,"ticket:create"); Inputs.text(request.originalText());
        if (request.category()==TicketCategory.ACCESS_REQUEST) {
            policy.require(user,"ROLE_REQUESTER");
            Inputs.text(request.permissionName());
            if (request.permissionName()==null || request.permissionName().isBlank() || request.permissionName().length()>120)
                throw BusinessException.invalid("请选择申请权限");
            if (mapper.permission(request.permissionName().strip())==null) throw BusinessException.invalid("请选择有效的系统权限");
        } else if (request.permissionName()!=null && !request.permissionName().isBlank()) {
            throw BusinessException.invalid("仅权限申请可以填写权限名称");
        }
        var tokens=request.attachmentTokens()==null ? List.<String>of() : request.attachmentTokens();
        if (new HashSet<>(tokens).size()!=tokens.size()) throw BusinessException.invalid("不能重复选择图片");
        var digestFields=new ArrayList<Object>(Arrays.asList(request.originalText(), request.category().name(),
                request.relatedTicketId(), new TreeSet<>(tokens)));
        if (request.category()==TicketCategory.ACCESS_REQUEST) digestFields.add(request.permissionName().strip());
        String digest=AttachmentService.hash(encode(digestFields).getBytes(StandardCharsets.UTF_8));
        var existing=mapper.findRequest(request.requestId());
        if (existing!=null) return retry(user,existing,digest);
        try {
            return tx.execute(s -> {
                if (request.relatedTicketId()!=null) {
                    var related=mapper.find(request.relatedTicketId());
                    if (related==null || !user.getId().equals(related.requesterId) || !"COMPLETED".equals(related.status))
                        throw BusinessException.invalid("仅能关联本人已完成工单");
                }
                var row=new TicketRow(); row.requestId=request.requestId(); row.requesterId=user.getId();
                row.originalText=request.originalText(); row.creationDigest=digest;
                row.initialCategory=request.category().name(); row.category=row.initialCategory;
                row.relatedTicketId=request.relatedTicketId(); mapper.insert(row);
                if (request.category()==TicketCategory.ACCESS_REQUEST)
                    if (mapper.insertPermissionRequest(row.id,user.getId(),request.permissionName().strip())!=1)
                        throw BusinessException.conflict("权限已变更，请重新选择");
                var event=event(user,row,"CREATED",null,Map.of("category",row.category));
                attachments.bind(user,row.id,event.id,tokens);
                notify(user,row,event,"TICKET_CREATED",mapper.coordinators());
                return mapper.find(row.id);
            });
        } catch (DuplicateKeyException ex) {
            var saved=mapper.findRequest(request.requestId());
            if (saved==null) throw ex;
            return retry(user,saved,digest);
        }
    }

    private TicketRow retry(SessionUser user, TicketRow row,String digest) {
        if (!user.getId().equals(row.requesterId) || !digest.equals(row.creationDigest))
            throw BusinessException.conflict("提交编号已使用，且提交内容不一致");
        return row;
    }

    public String scope(SessionUser user) {
        if (policy.has(user,"ticket:read:all")) return "ALL";
        if (policy.has(user,"ticket:read:own")) return "OWN";
        if (policy.has(user,"ticket:read:assigned")) return "ASSIGNED";
        throw new org.springframework.security.access.AccessDeniedException("No read permission");
    }

    public record Page(List<TicketRow> items,long total,int page,int size) { }
    public Page list(SessionUser user,int page,int size,String status,String category,String requestId,boolean unassigned) {
        if (page<1 || page>100000 || size<1 || size>100) throw BusinessException.invalid("分页参数不合法");
        if (status!=null && !status.isEmpty()) parseStatus(status);
        if (category!=null && !category.isEmpty()) parseCategory(category);
        return tx.execute(s -> new Page(mapper.list(user.getId(),scope(user),status,category,requestId,unassigned,(page-1)*size,size),
                mapper.count(user.getId(),scope(user),status,category,requestId,unassigned),page,size));
    }
    public record Detail(TicketRow ticket,List<EventRow> events,List<com.ticketing.attachment.model.AttachmentRow> attachments,
                         Set<String> actions,boolean relatedReadable,UserPermissionRow permissionRequest) { }
    public Detail detail(SessionUser user,Long id) {
        return tx.execute(s -> {
            var row=mapper.find(id); policy.readable(user,row);
            boolean related=row.relatedTicketId!=null && policy.canRead(user,mapper.find(row.relatedTicketId));
            return new Detail(row,policy.has(user,"ticket:history:read")?mapper.events(id):List.of(),
                    policy.has(user,"attachment:read")?attachments.list(id):List.of(),actions(user,row),related,"ACCESS_REQUEST".equals(row.category)?mapper.permissionRequest(id):null);
        });
    }
    public List<PermissionOption> permissions(SessionUser user) {
        if (policy.has(user,"ROLE_REQUESTER")) policy.require(user,"ticket:create");
        else { policy.require(user,"ROLE_COORDINATOR"); policy.require(user,"ticket:access:approve"); }
        return mapper.permissions();
    }
    public List<HandlerOption> handlers(SessionUser user) {
        policy.require(user,"ticket:assign"); return mapper.handlers();
    }
    public Map<String,Long> counts(SessionUser user) {
        return tx.execute(s -> {
            var result=new LinkedHashMap<String,Long>();
            for (var state:TicketStatus.values()) result.put(state.name(),mapper.count(user.getId(),scope(user),state.name(),null,null,false));
            if (policy.has(user,"ticket:read:all")) result.put("UNASSIGNED",mapper.count(user.getId(),"ALL",null,null,null,true));
            return result;
        });
    }

    public TicketRow assign(SessionUser user,Long id,Assign request) {
        return change(user,id,request.version(),"ticket:assign",row -> {
            regularTicket(row);
            if (Objects.equals(row.assigneeId,request.assigneeId())) return;
            Long old=row.assigneeId;
            if (old!=null) { policy.require(user,"ticket:reassign"); Inputs.text(request.reason()); }
            else if (request.reason()!=null && !request.reason().isEmpty()) Inputs.text(request.reason());
            if (mapper.handler(request.assigneeId())==null) throw BusinessException.invalid("处理人必须是有效的协调员或技术支持");
            row.assigneeId=request.assigneeId(); save(row);
            var event=event(user,row,old==null?"ASSIGNED":"REASSIGNED",request.reason(),
                    Map.of("fromAssignee",old==null?"":old.toString(),"toAssignee",row.assigneeId.toString()));
            // Coordinators retain all-ticket scope, so their existing notifications remain valid.
            if (old!=null) {
                var oldUser=mapper.handler(old);
                if (oldUser==null || !"COORDINATOR".equals(oldUser.role())) notifications.invalidate(row.id,old);
            }
            notify(user,row,event,old==null?"TICKET_ASSIGNED":"TICKET_REASSIGNED",Arrays.asList(row.assigneeId,row.requesterId));
        });
    }
    public TicketRow category(SessionUser user,Long id,Category request) {
        return change(user,id,request.version(),"ticket:category:update",row -> {
            if ("ACCESS_REQUEST".equals(row.category) || request.category()==TicketCategory.ACCESS_REQUEST)
                throw BusinessException.invalid("权限申请不能与普通工单互改分类，请重新提交申请");
            Inputs.text(request.reason());
            if (row.category.equals(request.category().name())) return;
            String old=row.category; row.category=request.category().name(); save(row);
            event(user,row,"CATEGORY_CHANGED",request.reason(),Map.of("from",old,"to",row.category));
        });
    }
    public TicketRow start(SessionUser user,Long id,Version request) {
        return change(user,id,request.version(),"ticket:start",row -> {
            regularTicket(row); policy.handler(user,row); policy.state(row,"PENDING","REWORK");
            transition(user,row,"IN_PROGRESS","STARTED",null,Map.of());
        });
    }
    public TicketRow comment(SessionUser user,Long id,Comment request) {
        return change(user,id,request.version(),"ticket:comment",row -> {
            Inputs.text(request.content());
            var tokens=request.attachmentTokens()==null?List.<String>of():request.attachmentTokens();
            if (!tokens.isEmpty()) policy.requester(user,row);
            save(row);
            var event=event(user,row,user.getId().equals(row.requesterId)?"SUPPLEMENTED":"COMMENTED",request.content(),Map.of());
            attachments.bind(user,row.id,event.id,tokens);
            var recipients=new ArrayList<>(Arrays.asList(row.requesterId,row.assigneeId));
            recipients.addAll(mapper.coordinators()); notify(user,row,event,"NEW_MESSAGE",recipients);
        });
    }
    public TicketRow result(SessionUser user,Long id,Result request) {
        return change(user,id,request.version(),"ticket:result:submit",row -> {
            regularTicket(row); policy.handler(user,row); policy.state(row,"IN_PROGRESS"); Inputs.text(request.content());
            if (!resolutions(row.category).contains(request.resolutionCode())) throw BusinessException.invalid("处理结论不适用于当前分类");
            row.resolutionCode=request.resolutionCode();
            var event=transition(user,row,"AWAITING_CONFIRMATION","RESULT_SUBMITTED",request.content(),Map.of("resolution",row.resolutionCode));
            notify(user,row,event,"RESULT_SUBMITTED",List.of(row.requesterId));
        });
    }
    private void regularTicket(TicketRow row) {
        if ("ACCESS_REQUEST".equals(row.category)) throw BusinessException.invalid("权限申请请由协调员直接审批");
    }
    public TicketRow approve(SessionUser user,Long id,Result request) {
        policy.require(user,"ROLE_COORDINATOR");
        return change(user,id,request.version(),"ticket:access:approve",row -> {
            if (!"ACCESS_REQUEST".equals(row.category)) throw BusinessException.invalid("仅权限申请可以审批");
            policy.state(row,"PENDING","IN_PROGRESS","REWORK","AWAITING_CONFIRMATION");
            if (!Set.of("APPROVED","REJECTED").contains(request.resolutionCode())) throw BusinessException.invalid("请选择通过或拒绝");
            Inputs.text(request.content());
            var application=mapper.permissionRequest(id);
            String code=application==null?null:application.permissionCode;
            if ("APPROVED".equals(request.resolutionCode())) {
                if (code==null) code=request.permissionCode();
                else if (request.permissionCode()!=null && !code.equals(request.permissionCode()))
                    throw BusinessException.invalid("不能更换提交者申请的权限");
                if (code==null || mapper.permission(code)==null) throw BusinessException.invalid("请为旧申请选择要授予的有效权限");
            }
            if (application==null) {
                // Legacy tickets need a row even when rejected; no permission is granted by this insertion.
                mapper.insertLegacyPermissionRequest(id,row.requesterId,row.originalText.substring(0,Math.min(120,row.originalText.length())));
            }
            if (mapper.reviewPermissionRequest(id,request.resolutionCode(),user.getId(),code)!=1)
                throw BusinessException.conflict("申请已审批，请刷新");
            row.assigneeId=user.getId(); row.resolutionCode=request.resolutionCode();
            row.completedAt=LocalDateTime.now(ZoneOffset.UTC);
            var event=transition(user,row,"COMPLETED","RESULT_SUBMITTED",request.content(),Map.of("resolution",row.resolutionCode));
            notify(user,row,event,"TICKET_COMPLETED",List.of(row.requesterId));
        });
    }
    public TicketRow confirm(SessionUser user,Long id,Version request) {
        return change(user,id,request.version(),"ticket:confirm",row -> {
            regularTicket(row); policy.requester(user,row); policy.state(row,"AWAITING_CONFIRMATION");
            row.completedAt=LocalDateTime.now(ZoneOffset.UTC);
            var event=transition(user,row,"COMPLETED","COMPLETED",null,Map.of());
            var recipients=new ArrayList<>(mapper.coordinators()); recipients.add(row.assigneeId);
            notify(user,row,event,"TICKET_COMPLETED",recipients);
        });
    }
    public TicketRow rework(SessionUser user,Long id,Rework request) {
        return change(user,id,request.version(),"ticket:rework",row -> {
            regularTicket(row); policy.requester(user,row); policy.state(row,"AWAITING_CONFIRMATION"); Inputs.text(request.reason());
            row.resolutionCode=null;
            var event=transition(user,row,"REWORK","REWORK_REQUESTED",request.reason(),Map.of());
            var recipients=new ArrayList<>(mapper.coordinators()); recipients.add(row.assigneeId);
            notify(user,row,event,"REWORK_REQUESTED",recipients);
        });
    }
    public Set<String> resolutions(String category) {
        return switch (category) {
            case "SYSTEM_FAILURE" -> Set.of("RESOLVED");
            case "ACCESS_REQUEST" -> Set.of("APPROVED","REJECTED");
            case "FEATURE_SUGGESTION" -> Set.of("ACCEPTED","DEFERRED","REJECTED");
            case "USAGE_QUESTION" -> Set.of("ANSWERED");
            default -> Set.of("RESOLVED","ANSWERED","REJECTED");
        };
    }
    private TicketRow change(SessionUser user,Long id,Long version,String permission,java.util.function.Consumer<TicketRow> operation) {
        policy.require(user,permission);
        return tx.execute(s -> {
            var row=mapper.lock(id); policy.writable(user,row,version); operation.accept(row); return mapper.find(id);
        });
    }
    private void save(TicketRow row) {
        if (mapper.update(row)!=1) throw BusinessException.conflict("工单已更新，请刷新");
        row.version++;
    }
    private EventRow transition(SessionUser user,TicketRow row,String to,String type,String content,Map<String,String> extra) {
        String old=row.status; row.status=to; save(row);
        var changes=new LinkedHashMap<>(extra); changes.put("from",old); changes.put("to",to);
        return event(user,row,type,content,changes);
    }
    private EventRow event(SessionUser user,TicketRow row,String type,String content,Object changes) {
        var event=new EventRow(); event.ticketId=row.id; event.actorId=user.getId();
        event.eventType=type; event.content=content; event.changes=encode(changes); mapper.addEvent(event); return event;
    }
    private void notify(SessionUser user,TicketRow row,EventRow event,String type,Collection<Long> recipients) {
        notifications.publish(row.id,event.id,type,user.getId(),recipients);
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); } catch (JsonProcessingException ex) { throw new IllegalStateException(ex); }
    }
    private Set<String> actions(SessionUser user,TicketRow row) {
        var result=new LinkedHashSet<String>();
        if ("COMPLETED".equals(row.status)) return result;
        if (policy.has(user,"ticket:comment")) result.add("comment");
        if ("ACCESS_REQUEST".equals(row.category)) {
            if (policy.has(user,"ROLE_COORDINATOR") && policy.has(user,"ticket:access:approve")) result.add("approve");
            return result;
        }
        if (policy.has(user,"ticket:assign")) result.add("assign");
        if (policy.has(user,"ticket:category:update")) result.add("category");
        if (Objects.equals(user.getId(),row.assigneeId)) {
            if (policy.has(user,"ticket:start") && Set.of("PENDING","REWORK").contains(row.status)) result.add("start");
            if (policy.has(user,"ticket:result:submit") && "IN_PROGRESS".equals(row.status)) result.add("result");
        }
        if (Objects.equals(user.getId(),row.requesterId) && "AWAITING_CONFIRMATION".equals(row.status)) {
            if (policy.has(user,"ticket:confirm")) result.add("confirm");
            if (policy.has(user,"ticket:rework")) result.add("rework");
        }
        return result;
    }
    private void parseStatus(String value) {
        try { TicketStatus.valueOf(value); } catch (IllegalArgumentException ex) { throw BusinessException.invalid("未知状态"); }
    }
    private void parseCategory(String value) {
        try { TicketCategory.valueOf(value); } catch (IllegalArgumentException ex) { throw BusinessException.invalid("未知类型"); }
    }
}
