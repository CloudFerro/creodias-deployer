package com.cloudferro.copernicus.experiments.controller;

import com.cloudferro.copernicus.experiments.service.SolutionDeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class DeploymentController {

    private final SolutionDeploymentService deploymentService;

    @RequestMapping(value = "/deploy/{solutionId}/{revisionId}", method = RequestMethod.GET)
    public String initDeploy(@PathVariable("solutionId") String solutionId, @PathVariable("revisionId") String revisionId,
                         Model model) {
        var deployment = new DeploymentDto();
        deployment.setSolutionId(solutionId);
        deployment.setRevisionId(revisionId);
        deployment.setNamespaceName(deploymentService.getDefaultNamespaceName(solutionId, revisionId));
        model.addAttribute("deployment", deployment);
        return "deployment_form";
    }

    @PostMapping("/deploy")
    public String deploy(@Valid DeploymentDto deployment, Model model) {
        try {
            var services = deploymentService.deploySolution(deployment.getSolutionId(), deployment.getRevisionId(),
                                                            deployment.getConfig(), deployment.getNamespaceName());
            var links = services.stream()
                    .map(ServiceDto::fromDomain)
                    .toList();
            model.addAttribute("services", links);
            Thread.sleep(5000);
            return "success";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "error";
        }
    }
}
