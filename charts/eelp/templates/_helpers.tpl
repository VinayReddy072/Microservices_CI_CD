{{- define "eelp.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "eelp.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{- define "eelp.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: eelp
{{- end }}

{{- define "eelp.image" -}}
{{ .registry }}/{{ .name }}:{{ .tag }}
{{- end }}

{{- define "eelp.readinessProbe" -}}
readinessProbe:
  httpGet:
    path: /actuator/health
    port: {{ .port }}
  initialDelaySeconds: {{ .delay | default 30 }}
  periodSeconds: 10
  failureThreshold: 8
{{- end }}

{{- define "eelp.livenessProbe" -}}
livenessProbe:
  httpGet:
    path: /actuator/health
    port: {{ .port }}
  initialDelaySeconds: {{ .delay | default 90 }}
  periodSeconds: 20
{{- end }}
