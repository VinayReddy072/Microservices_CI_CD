{{/*
  ============================================================
  EELP Helm Chart — Template Helpers
  ============================================================
*/}}

{{/*
  Expand chart name.
*/}}
{{- define "eelp.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
  Create a default fully qualified app name.
*/}}
{{- define "eelp.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
  Common labels applied to all resources.
*/}}
{{- define "eelp.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: eelp
{{- end }}

{{/*
  Build a full image reference from global registry + service image name + tag.
  Usage: {{ include "eelp.image" (dict "registry" .Values.global.imageRegistry "name" .Values.gateway.image "tag" .Values.global.imageTag) }}
*/}}
{{- define "eelp.image" -}}
{{ .registry }}/{{ .name }}:{{ .tag }}
{{- end }}

{{/*
  Standard readiness probe for Spring Boot Actuator.
  Usage: {{ include "eelp.readinessProbe" (dict "port" 8080 "delay" 30) }}
*/}}
{{- define "eelp.readinessProbe" -}}
readinessProbe:
  httpGet:
    path: /actuator/health
    port: {{ .port }}
  initialDelaySeconds: {{ .delay | default 30 }}
  periodSeconds: 10
  failureThreshold: 8
{{- end }}

{{/*
  Standard liveness probe for Spring Boot Actuator.
*/}}
{{- define "eelp.livenessProbe" -}}
livenessProbe:
  httpGet:
    path: /actuator/health
    port: {{ .port }}
  initialDelaySeconds: {{ .delay | default 90 }}
  periodSeconds: 20
{{- end }}
