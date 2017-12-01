#!/usr/bin/env groovy

import hudson.model.Action

import com.cloudbees.workflow.flownode.FlowNodeUtil
import com.cloudbees.workflow.rest.external.StatusExt

import org.jenkinsci.plugins.workflow.graph.FlowNode
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode
import org.jenkinsci.plugins.workflow.actions.LabelAction


@NonCPS
def convertParameters(parameters) {
    def parametersConverted = new ArrayList<hudson.model.ParameterValue>()
    for (param in parameters) {
        def key = param.key.trim()
        if (param.value instanceof Boolean) {
            parametersConverted.add(new BooleanParameterValue(key.toString(), param.value))
        }
        else {
            parametersConverted.add(new StringParameterValue(key.toString(), param.value.toString()))
        }
    }

    return parametersConverted
}

def __flowNodeHasLabelAction(FlowNode flowNode){
    def actions = flowNode.getActions()

    for (Action action: actions){
        if (action instanceof LabelAction) {
            return true
        }
    }

    return false
}

def __getBuildStages(List<FlowNode> flowNodes, data = [startNodes: [], stages: []]) {
    def currentFlowNode = null

    for (FlowNode flowNode: flowNodes){
        currentFlowNode = flowNode
        if (flowNode instanceof StepEndNode) {
		def startNode = flowNode.getStartNode()
		if (__flowNodeHasLabelAction(startNode)) {
			data.startNodes.add(0, startNode)
			data.stages.add(0, [name: startNode.getDisplayName(), status: FlowNodeUtil.getStatus(flowNode)])
		}
        }
	else if(flowNode instanceof StepStartNode && __flowNodeHasLabelAction(flowNode) && !data.startNodes.contains(flowNode)) {
		data.startNodes.add(0, flowNode)
		data.stages.add(0, [name: flowNode.getDisplayName(), status: StatusExt.IN_PROGRESS])
        }
    }

    if (currentFlowNode == null) {
        return data.stages
    }

    return __getBuildStages(currentFlowNode.getParents(), data)
}

def getBuildInfo(build){
    def rawBuild = build.getRawBuild()
    def execution = rawBuild.getExecution()
    def executionHeads = execution.getCurrentHeads()
    def data = [
	status: build.result,
	stages: __getBuildStages(executionHeads)
    ]
    return data
}

def getBuildCurrentStage(build){
    def data = getBuildInformations(build)
    return data.stages.get(data.stages.size() - 1);
}

def call(build) {
    return getBuildInfo(build)
}

/*
stage('Stage 1') {
    println "Current build"
    println getBuildInformations(currentBuild)

    println "Current build stage"
    println getBuildCurrentStage(currentBuild)

    println "Previous build"
    println getBuildInformations(currentBuild.getPreviousBuild())
}

stage('Stage 2') {
    println "Current build"
    println getBuildInformations(currentBuild)

    println "Current build stage"
    println getBuildCurrentStage(currentBuild)

    def buildOtherPipeline = build job: "Other pipeline", parameters: convertParameters(params), propagate: false
    println "Current build for buildOtherPipeline"
    println getBuildInformations(buildOtherPipeline)

    println "Previous build for buildOtherPipeline"
    println getBuildInformations(buildOtherPipeline.getPreviousBuild())
}

*/
