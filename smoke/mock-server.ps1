#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Offline, deterministic TVBox-compatible mock for QBox smoke tests.
.DESCRIPTION
  Serves a static configuration, three synthetic films and one placeholder poster.
  No real source, account, or live feed is included. Uses only built-in PowerShell/.NET APIs.
.EXAMPLE
  pwsh smoke/mock-server.ps1
  # Keep it running, then build with -PqboxTestConfig=http://10.0.2.2:8765/config.json.
#>

[CmdletBinding()]
param(
  [string]$HostAddress = '127.0.0.1',
  [ValidateRange(1024, 65535)]
  [int]$Port = 8765,
  [string]$PublicBaseUrl = 'http://10.0.2.2:8765'
)

$ErrorActionPreference = 'Stop'
$PublicBaseUrl = $PublicBaseUrl.TrimEnd('/')
$logoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\branding\qbox-logo.png'))
if (Test-Path -LiteralPath $logoPath) {
  $png = [IO.File]::ReadAllBytes($logoPath)
} else {
  # 1x1 grey fallback keeps the mock usable when the brand asset is not checked out.
  $png = [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=')
}

$movies = @(
  @{ id = 'mock-moon';   name = 'Mock Moon';   note = 'Sci-Fi / 2026' },
  @{ id = 'mock-harbor'; name = 'Harbor Watch'; note = 'Drama / 2025'  },
  @{ id = 'mock-valley'; name = 'Valley Letters'; note = 'Mystery / 2024' }
)

function Send-Bytes($Response, [byte[]]$Bytes, [string]$ContentType, [int]$StatusCode = 200) {
  $Response.StatusCode = $StatusCode
  $Response.ContentType = $ContentType
  $Response.ContentLength64 = $Bytes.LongLength
  $Response.Headers['Cache-Control'] = 'no-store'
  $Response.OutputStream.Write($Bytes, 0, $Bytes.Length)
}

function Make-Config {
  $json = "{`"sites`":[{`"key`":`"qbox-mock`",`"name`":`"QBox Demo Theater`",`"type`":0,`"api`":`"$PublicBaseUrl/rss`",`"searchable`":1,`"quickSearch`":1,`"changeable`":0}],`"rules`":[],`"lives`":[]}"
  return [Text.Encoding]::UTF8.GetBytes($json)
}

function Make-Xml {
  $classes = '<ty id="movie">Movie</ty><ty id="series">Series</ty><ty id="documentary">Documentary</ty>'
  $videos = ($movies | ForEach-Object {
    "<video><id>$($_.id)</id><name>$($_.name)</name><type>Movie</type><pic>$PublicBaseUrl/poster/$($_.id).png</pic><note>$($_.note)</note><year>2026</year><area>China</area><des>QBox offline placeholder; used only for demo and screenshot regression.</des></video>"
  }) -join ''
  $xml = "<?xml version=`"1.0`" encoding=`"utf-8`"?><rss version=`"4.0`"><class>$classes</class><list page=`"1`" pagecount=`"1`">$videos</list></rss>"
  return [Text.Encoding]::UTF8.GetBytes($xml)
}

$listener = [System.Net.HttpListener]::new()
$prefix = "http://$($HostAddress):$($Port)/"
$listener.Prefixes.Add($prefix)
$listener.Start()
Write-Host "QBox mock listening on $prefix (emulator URL: $PublicBaseUrl)"
try {
  while ($listener.IsListening) {
    $ctx = $listener.GetContext()
    $path = [Uri]::UnescapeDataString($ctx.Request.Url.AbsolutePath)
    # 记录 Host 头：HttpListener 在 Linux 上按 Host 匹配前缀，排查 404 时这是唯一线索。
    Write-Host "$(Get-Date -Format o) $($ctx.Request.HttpMethod) $path host=$($ctx.Request.Headers['Host'])"
    try {
      if ($path -eq '/config.json') {
        Send-Bytes $ctx.Response (Make-Config) 'application/json; charset=utf-8'
      } elseif ($path -eq '/rss') {
        Send-Bytes $ctx.Response (Make-Xml) 'application/xml; charset=utf-8'
      } elseif ($path -match '^/poster/[A-Za-z0-9_-]+\.png$') {
        Send-Bytes $ctx.Response $png 'image/png'
      } elseif ($path -in @('/', '/health')) {
        Send-Bytes $ctx.Response ([Text.Encoding]::UTF8.GetBytes("ok`n")) 'text/plain; charset=utf-8'
      } else {
        Send-Bytes $ctx.Response ([Text.Encoding]::UTF8.GetBytes("not found`n")) 'text/plain; charset=utf-8' 404
      }
    } catch {
      try { Send-Bytes $ctx.Response ([Text.Encoding]::UTF8.GetBytes("mock error`n")) 'text/plain; charset=utf-8' 500 } catch { }
    } finally {
      try { $ctx.Response.Close() } catch { }
    }
  }
} finally {
  if ($listener.IsListening) { $listener.Stop() }
  $listener.Close()
}
