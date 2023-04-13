# Playground-Deployer

## Introduction
The architecture of Graphene is designed to support the integration of many different 
deployment services for execution environments. Each deployment service is a separate 
microservice running inside the Graphene cluster transforming the pipeline 
definition into deployment artifacts. 

CREODIAS deployer is an implementation of such deployment service. It uses Kubernates API in order to deploy solution 
to a remote cluster running in CREODIAS cloud infrastructure.

CREODIAS deployment service takes the following cases into account:
* the solution can be just a single model (= only one node)
* the solution can be a pipeline
* the pipeline can contain a shared folder node

This deployer exposes a REST API service initiating deployment process as well as GUI which allows providing kube.config 
file and modifying namespace name for the deployment.

## Input parameters
Each deployment service is called with the following input parameters:
* solutionId
* revisionId
* JWT authentication token

SolutionID and revisionID together uniquely identify the pipeline to deploy. After
checking the JWT authentication to ensure that the request is genuine, the file 
blueprint.json, which contains the pipeline topology can be retrieved from the 
persistence service. If the solution is not a pipeline but a single model, 
the handling is described further down.

After validation jwtToken is set as a cookie allowing passing security context in subsequent calls.

## Processing the Pipeline definition blueprint.json 
The file **blueprint.json** contains the nodes and edges forming the pipeline. For 
the deployment, only the nodes are relevant, not the edges. The most important
attribute of a node is the **docker image URL**: it is the reference to the public
or private registry where the asset provider has stored the image.

The deployer iterates over all nodes:
* if the node is a **shared folder provider**, a kubernetes persistent volume claim is generated _and the node is removed from blueprint.json_
* for any other type, the **docker image URL** is read from **blueprint.json** 
* one **service.yaml** and one **deployment.yaml** is created for each node
* the **protobuf file** for the node is fetched from the database and added to the solution.zip
* the deployer automatically adds the **orchestrator node** to the deyloyment. The orchestrator is importatnt for execution as it dispatches the messages to the nodes along the edges.

CREODIAS deployer automatically initialises orchestrator node and runs the solution once the orchestrator reports Ready state.

**Important:** The persistent volume for a shared folder is expected to have accessMode **ReadWriteMany** because all node of a pipeline need to have the same level of access

## Processing for a single model

For a single model, the docker image URL is read from the database and exactly one service.yaml and one deployment.yaml is created.


