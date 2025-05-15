package com.pino.intellijmockitotools;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.pino.intellijmockitotools.utils.ClipboardUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseAction extends AnAction {

    abstract String generateCode(String className, String methodName, List<String> parameterTypeList, String returnType);

    @Override
    public void update(@NotNull AnActionEvent event) {
        // 顯示選單時觸發
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        var editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        var virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            return;
        }

        var fileType = virtualFile.getFileType().getName();
        if (!"JAVA".equalsIgnoreCase(fileType)) {
            return;
        }

        var psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }

        var project = event.getProject();
        if (project == null) {
            return;
        }

        var psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (psiElement == null) {
            return;
        }

        var parentClassName = psiElement.getParent().getClass().getSimpleName();

        PsiMethod psiMethod;
        switch (parentClassName) {
            case "PsiMethodImpl":
                psiMethod = ((PsiMethod) psiElement.getParent());
                doPsiMethodFlow(project, psiMethod);
                break;
            case "PsiReferenceExpressionImpl":
                psiMethod = (PsiMethod) ((PsiReference) psiElement.getParent()).resolve();
                doPsiMethodFlow(project, psiMethod);
                break;
            default:
                showNotSupportedNotification(project);
                break;
        }
    }

    private void doPsiMethodFlow(Project project, PsiMethod psiMethod) {
        var className = ((PsiClassImpl) psiMethod.getParent()).getName();
        var methodName = psiMethod.getName();
        var parameterTypeList = new ArrayList<String>();

        var parameters = psiMethod.getParameterList().getParameters();
        for (PsiParameter param : parameters) {
            var paramPsiType = param.getType();
            String paramType;
            if (paramPsiType instanceof PsiPrimitiveType primitiveType) {
                paramType = primitiveType.getName();
            } else {
                paramType = ((PsiClassReferenceType) paramPsiType).getClassName();
            }
            parameterTypeList.add(paramType);
        }

        var returnPsiType = psiMethod.getReturnType();
        var returnType = returnPsiType != null ? ((PsiClassReferenceType) returnPsiType).getClassName() : "void";

        var result = generateCode(className, methodName, parameterTypeList, returnType);
        ClipboardUtils.copyToClipboard(result);
        showSuccessNotification(project);
    }

    protected String toMockType(String type) {
        return switch (type) {
            case "String" -> "anyString()";
            case "Integer", "int" -> "anyInt()";
            case "Long", "long" -> "anyLong()";
            case "Double", "double" -> "anyDouble()";
            case "Float", "float" -> "anyFloat()";
            case "Boolean", "boolean" -> "anyBoolean()";
            case "Byte", "byte" -> "anyByte()";
            case "Char", "char" -> "anyChar()";
            case "List" -> "anyList()";
            case "Set" -> "anySet()";
            case "Map" -> "anyMap()";
            case "Collection" -> "anyCollection()";
            default -> "any(%s.class)".formatted(type);
        };
    }

    protected String toMockValue(String type) {
        return switch (type) {
            case "String" -> "\"\"";
            case "Integer", "int" -> "0";
            case "Long", "long" -> "0L";
            case "Double", "double" -> "0D";
            case "Float", "float" -> "0F";
            case "Boolean", "boolean" -> "true";
            case "Byte", "byte" -> "new Byte()";
            case "Char", "char" -> "'A'";
            case "List" -> "new ArrayList<>()";
            case "Set" -> "new HashSet<>()";
            case "Map" -> "new HashMap<String, Object>()";
            default -> "new %s()".formatted(type);
        };
    }

    private void showNotSupportedNotification(Project project) {
        var groupId = "Mockito tools: Not supported";
        var title = "Not supported!";
        var content = "This feature is not supported";
        var notification = new Notification(groupId, title, content, NotificationType.WARNING);
        Notifications.Bus.notify(notification, project);
    }

    private void showSuccessNotification(Project project) {
        var groupId = "Mockito tools: Code copied to clipboard";
        var title = "Code copied to clipboard";
        var notification = new Notification(groupId, title, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification, project);
    }
}
