package cn.edu.hziee.cams.controller;

import cn.edu.hziee.cams.util.JsonUtil;
import cn.edu.hziee.cams.entity.KeyValue;
import cn.edu.hziee.cams.entity.MyForm;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
//import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by HuangChao on 2021/3/25
 */
@Controller
@RequestMapping
public class ActivitiController {
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FormService formService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;


    @RequestMapping("index")
    public String toIndex(org.springframework.ui.Model model) {
        List<Model> list = repositoryService.createModelQuery().list();
        model.addAttribute("list", list);
        return "index";
    }


    /**
     *
     * @param request
     * @param response
     * @return String
     */
    @RequestMapping("createModel")
    public String createModel(HttpServletRequest request, HttpServletResponse response) {

        String name = "????????????";
        String description = "????????????????????????";

        String id = null;
        try {
            Model model = repositoryService.newModel();
            String key = name;
            //?????????
            String revision = "1";
            ObjectNode modelNode = objectMapper.createObjectNode();
            modelNode.put(ModelDataJsonConstants.MODEL_NAME, name);
            modelNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
            modelNode.put(ModelDataJsonConstants.MODEL_REVISION, revision);

            model.setName(name);
            model.setKey(key);
            //???????????? ???????????????????????????
            //model.setCategory(category);

            model.setMetaInfo(modelNode.toString());

            repositoryService.saveModel(model);
            id = model.getId();

            //??????ModelEditorSource
            ObjectNode editorNode = objectMapper.createObjectNode();
            editorNode.put("id", "canvas");
            editorNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = objectMapper.createObjectNode();
            stencilSetNode.put("namespace",
                    "http://b3mn.org/stencilset/bpmn2.0#");
            editorNode.put("stencilset", stencilSetNode);
            repositoryService.addModelEditorSource(id, editorNode.toString().getBytes("utf-8"));

            response.sendRedirect(request.getContextPath() + "/static/modeler.html?modelId=" + id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "index";
    }


    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO ??????????????????
     * @Date 15:32 2019/8/3
     * @Param [id]
     **/
    @RequestMapping("deploymentModel")
    @ResponseBody
    public com.alibaba.fastjson.JSONObject deploymentModel(String id) throws Exception {

        //????????????
        Model modelData = repositoryService.getModel(id);
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());

        if (bytes == null) {
            return JsonUtil.getFailJson("???????????????????????????????????????????????????????????????????????????");
        }
        JsonNode modelNode = modelNode = new ObjectMapper().readTree(bytes);

        BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        if (model.getProcesses().size() == 0) {
            return JsonUtil.getFailJson("???????????????????????????????????????????????????????????????");
        }
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);

        //????????????
        String processName = modelData.getName() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment()
                .name(modelData.getName())
                .addString(processName, new String(bpmnBytes, "UTF-8"))
                .deploy();
        modelData.setDeploymentId(deployment.getId());
        repositoryService.saveModel(modelData);
        return JsonUtil.getSuccessJson("??????????????????");
    }

    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO ??????????????????
     * @Date 15:45 2019/8/3
     * @Param []
     **/
    @RequestMapping("startPage")
    public String startPage(org.springframework.ui.Model model) {
        //??????????????????
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().list();
        model.addAttribute("list", list);
        return "startPage";
    }


    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO ??????????????????????????????
     * @Date 16:47 2019/8/3
     * @Param []
     **/
    @RequestMapping("startProcess/{id}")
    public String startProcess(@PathVariable("id") String id, org.springframework.ui.Model model) {

        //??????????????????ID?????????????????????????????????????????????
        StartFormData startFormData = formService.getStartFormData(id);
        List<FormProperty> formProperties = startFormData.getFormProperties();

        //????????????ID
        model.addAttribute("processesId", id);
        model.addAttribute("form", formProperties);

        return "startProcess";
    }


    /**
     * @Description: ????????????????????????
     * @Param: [id]
     * @return: com.alibaba.fastjson.JSONObject
     * @Author: Mr.MRC
     * @Date: 2019/7/25  11:26
     */
    @RequestMapping("startProcesses")
    @ResponseBody
    public com.alibaba.fastjson.JSONObject startProcesses(@RequestParam Map<String, Object> param) {

        String processesId = (String) param.get("processesId");
        //??????????????? ????????????
        String userId = (String) param.get("userId");

        if (StringUtils.isEmpty(processesId)) {
            return JsonUtil.getFailJson("????????????");
        }
        param.remove("processesId");

//        Execution last = runtimeService.createExecutionQuery().processInstanceBusinessKey(userId).processDefinitionId(processesId).singleResult();
//        if (null != last) {
//            return JsonUtil.getFailJson("?????????????????????");
//        }

        ProcessInstance pi = runtimeService.startProcessInstanceById(processesId, userId, param);

        if (null == pi) {
            return JsonUtil.getFailJson("?????????????????????");
        }
        return JsonUtil.getSuccessJson("?????????????????????");

    }


    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO ?????????????????????
     * @Date 14:13 2019/9/7
     * @Param [id]
     **/
    @RequestMapping("taskApproval/{id}")
    public String toTaskList(@PathVariable("id") String id, org.springframework.ui.Model model) {

        if (StringUtils.isNotEmpty(id)) {

            List<Task> list = taskService.createTaskQuery().taskAssignee(id).list();
            model.addAttribute("list", list);
        }

        return "taskApproval";
    }

    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO ????????????
     * @Date 14:45 2019/9/7
     * @Param [id, model]
     **/
    @RequestMapping("taskDetails/{taskId}")
    public String toTaskDetails(@PathVariable("taskId") String id, org.springframework.ui.Model model) {


        Map<String, Object> map = new HashMap<>();
        //????????????
        Task task = this.taskService.createTaskQuery().taskId(id).singleResult();
        String processInstanceId = task.getProcessInstanceId();

        TaskFormData taskFormData = this.formService.getTaskFormData(id);
        List<FormProperty> list = taskFormData.getFormProperties();
        map.put("task", task);
        map.put("list", list);
        map.put("history", assembleProcessForm(processInstanceId));

        model.addAllAttributes(map);

        return "taskDetails";
    }


    /**
     * @return com.alibaba.fastjson.JSONObject
     * @Author MRC
     * @Description //TODO ????????????
     * @Date 15:50 2019/9/7
     * @Param []
     **/
    @RequestMapping("completeTasks")
    @ResponseBody
    public JSONObject completeTasks(@RequestParam Map<String, Object> param) {


        String taskId = (String) param.get("taskId");

        if (StringUtils.isEmpty(taskId)) {
            return JsonUtil.getFailJson("????????????");
        }
        param.remove("taskId");

        Task task = this.taskService.createTaskQuery().taskId(taskId).singleResult();

        taskService.complete(task.getId(), param);


        return JsonUtil.getSuccessJson("???????????????");

    }


    /**
     * @return java.util.List<com.yckj.entity.MyForm>
     * @Author MRC
     * @Description ?????????????????????????????????
     * @Date 10:59 2019/8/5
     * @Param [processInstanceId]
     **/
    public List<MyForm> assembleProcessForm(String processInstanceId) {

        List<HistoricActivityInstance> historys = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();

        List<MyForm> myform = new ArrayList<>();

        for (HistoricActivityInstance activity : historys) {

            String actInstanceId = activity.getId();
            MyForm form = new MyForm();
            form.setActName(activity.getActivityName());
            form.setAssignee(activity.getAssignee());
            form.setProcInstId(activity.getProcessInstanceId());
            form.setTaskId(activity.getTaskId());
            //??????????????????

            List<KeyValue> maps = new LinkedList<>();

            List<HistoricDetail> processes = historyService.createHistoricDetailQuery().activityInstanceId(actInstanceId).list();
            for (HistoricDetail process : processes) {
                HistoricDetailVariableInstanceUpdateEntity pro = (HistoricDetailVariableInstanceUpdateEntity) process;

                KeyValue keyValue = new KeyValue();

                keyValue.setKey(pro.getName());
                keyValue.setValue(pro.getTextValue());

                maps.add(keyValue);
            }
            form.setProcess(maps);

            myform.add(form);
        }

        return myform;
    }

    /**
     * @return com.alibaba.fastjson.JSONObject
     * @Author MRC
     * @Description //TODO ???????????????
     * @Date 15:39 2019/9/7
     * @Param [processInstanceId]
     **/
    @RequestMapping("generateProcessImg")
    @ResponseBody
    public JSONObject generateProcessImg(String processInstanceId) throws IOException {

        //????????????????????????
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        //???????????????
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());

        ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
        ProcessDefinitionEntity definitionEntity = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

        List<HistoricActivityInstance> highLightedActivitList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
        //????????????id??????
        List<String> highLightedActivitis = new ArrayList<String>();

        //????????????id??????
        List<String> highLightedFlows = getHighLightedFlows(definitionEntity, highLightedActivitList);

        for (HistoricActivityInstance tempActivity : highLightedActivitList) {
            String activityId = tempActivity.getActivityId();
            highLightedActivitis.add(activityId);
        }
        //????????????
        InputStream imageStream = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivitis, highLightedFlows, "??????", "????????????", "??????", null, 2.0);
        BufferedImage bi = ImageIO.read(imageStream);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", bos);
        //???????????????
        byte[] bytes = bos.toByteArray();
//        BASE64Encoder encoder = new BASE64Encoder();
//        //?????????base64???
//        String png_base64 = encoder.encodeBuffer(bytes);
        //?????? \r\n
//        png_base64 = png_base64.replaceAll("\n", "").replaceAll("\r", "");

        bos.close();
        imageStream.close();
        return JsonUtil.getSuccessJson("success", null);
    }


    public List<String> getHighLightedFlows(
            ProcessDefinitionEntity processDefinitionEntity,
            List<HistoricActivityInstance> historicActivityInstances) {

        // ????????????????????????flowId
        List<String> highFlows = new ArrayList<String>();
        // ?????????????????????????????????
        for (int i = 0; i < historicActivityInstances.size() - 1; i++) {
            // ?????????????????????????????????
            ActivityImpl activityImpl = processDefinitionEntity
                    .findActivity(historicActivityInstances.get(i)
                            .getActivityId());
            // ?????????????????????????????????????????????
            List<ActivityImpl> sameStartTimeNodes = new ArrayList<ActivityImpl>();
            ActivityImpl sameActivityImpl1 = processDefinitionEntity
                    .findActivity(historicActivityInstances.get(i + 1)
                            .getActivityId());
            // ????????????????????????????????????????????????????????????
            sameStartTimeNodes.add(sameActivityImpl1);
            for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {
                // ?????????????????????
                HistoricActivityInstance activityImpl1 = historicActivityInstances
                        .get(j);
                // ?????????????????????
                HistoricActivityInstance activityImpl2 = historicActivityInstances
                        .get(j + 1);
                // ???????????????????????????????????????????????????????????????
                if (activityImpl1.getStartTime().equals(
                        activityImpl2.getStartTime())) {
                    ActivityImpl sameActivityImpl2 = processDefinitionEntity
                            .findActivity(activityImpl2.getActivityId());
                    sameStartTimeNodes.add(sameActivityImpl2);
                } else {
                    // ????????????????????????
                    break;
                }
            }
            // ?????????????????????????????????
            List<PvmTransition> pvmTransitions = activityImpl
                    .getOutgoingTransitions();
            // ???????????????????????????
            for (PvmTransition pvmTransition : pvmTransitions) {
                ActivityImpl pvmActivityImpl = (ActivityImpl) pvmTransition
                        .getDestination();
                // ?????????????????????????????????????????????????????????????????????????????????id?????????????????????
                if (sameStartTimeNodes.contains(pvmActivityImpl)) {
                    highFlows.add(pvmTransition.getId());
                }
            }
        }
        return highFlows;
    }
}
