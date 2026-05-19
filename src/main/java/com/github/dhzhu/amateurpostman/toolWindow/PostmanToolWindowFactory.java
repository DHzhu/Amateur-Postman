package com.github.dhzhu.amateurpostman.toolWindow;

import com.github.dhzhu.amateurpostman.codeinsight.ControllerRequestTopic;
import com.github.dhzhu.amateurpostman.ui.GrpcEditorPanel;
import com.github.dhzhu.amateurpostman.ui.PostmanToolWindowPanel;
import com.github.dhzhu.amateurpostman.ui.WebSocketPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/** Factory for creating the Amateur-Postman tool window */
public class PostmanToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JBTabbedPane protocolTabs = new JBTabbedPane();

        // HTTP tab
        PostmanToolWindowPanel httpPanel = new PostmanToolWindowPanel(project);
        Disposer.register(toolWindow.getDisposable(), httpPanel);
        protocolTabs.addTab("HTTP", httpPanel.createPanel());

        // WebSocket tab
        WebSocketPanel wsPanel = new WebSocketPanel(project);
        Disposer.register(toolWindow.getDisposable(), wsPanel::dispose);
        protocolTabs.addTab("WebSocket", wsPanel.createPanel());

        // gRPC tab
        GrpcEditorPanel grpcPanel = new GrpcEditorPanel(project);
        Disposer.register(toolWindow.getDisposable(), grpcPanel::dispose);
        protocolTabs.addTab("gRPC", grpcPanel.createPanel());

        var content = ContentFactory.getInstance().createContent(protocolTabs, "", false);
        toolWindow.getContentManager().addContent(content);

        // Subscribe to controller request events
        project.getMessageBus().connect(toolWindow.getDisposable()).subscribe(
                ControllerRequestTopic.INSTANCE.getREQUEST(),
                new ControllerRequestTopic.ControllerRequestListener() {
                    @Override
                    public void onRequestReady(@NotNull com.github.dhzhu.amateurpostman.models.HttpRequest request, @NotNull String name) {
                        httpPanel.loadExternalRequest(request, name);
                    }
                }
        );
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
