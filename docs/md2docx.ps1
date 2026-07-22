param(
  [string]$In  = "$PSScriptRoot\毕业设计论文.md",
  [string]$Out = "$PSScriptRoot\毕业设计论文.docx"
)
$ErrorActionPreference = 'Stop'

# Word 内置样式常量
$wdStyleTitle    = -63
$wdStyleHeading1 = -2
$wdStyleHeading2 = -3
$wdStyleHeading3 = -4
$wdStyleNormal   = -1
$wdStyleQuote    = -80
$wdFormatDocx    = 16   # wdFormatDocumentDefault (.docx)
$wdFormatDocx07  = 12   # wdFormatXMLDocument (Office2007 .docx)

function Clean-Inline([string]$s) {
  $s = $s -replace '\*\*(.+?)\*\*', '$1'   # 去粗体标记
  $s = $s -replace '`(.+?)`', '$1'         # 去行内代码反引号
  $s = $s -replace '!\[.*?\]\(.*?\)', ''   # 去图片语法
  $s = $s -replace '\[(.+?)\]\(.*?\)', '$1' # 链接保留文字
  return $s
}

$lines = [System.IO.File]::ReadAllLines((Resolve-Path $In), [System.Text.Encoding]::UTF8)

$word = New-Object -ComObject Word.Application
$word.Visible = $false
try {
  $doc = $word.Documents.Add()
  $sel = $word.Selection

  $i = 0
  $inCode = $false
  $codeBuf = @()

  function Flush-Code($buf) {
    if ($buf.Count -eq 0) { return }
    $sel.Style = $wdStyleNormal
    $sel.Font.Name = 'Consolas'
    $sel.Font.Size = 9
    foreach ($cl in $buf) {
      $sel.TypeText($cl)
      $sel.TypeParagraph()
    }
    $sel.Font.Name = ''
    $sel.Font.Size = 10.5
  }

  while ($i -lt $lines.Count) {
    $line = $lines[$i]

    # 代码围栏
    if ($line -match '^\s*```') {
      if (-not $inCode) { $inCode = $true; $codeBuf = @() }
      else { Flush-Code $codeBuf; $inCode = $false; $codeBuf = @() }
      $i++; continue
    }
    if ($inCode) { $codeBuf += $line; $i++; continue }

    # 表格：连续以 | 开头的行
    if ($line -match '^\s*\|') {
      $tbl = @()
      while ($i -lt $lines.Count -and $lines[$i] -match '^\s*\|') {
        $tbl += $lines[$i]; $i++
      }
      # 过滤分隔行 |---|
      $rows = @()
      foreach ($r in $tbl) {
        if ($r -match '^\s*\|[\s:\-\|]+\|\s*$') { continue }
        $cells = $r.Trim().Trim('|').Split('|') | ForEach-Object { (Clean-Inline $_).Trim() }
        $rows += ,$cells
      }
      if ($rows.Count -gt 0) {
        $nc = ($rows | ForEach-Object { $_.Count } | Measure-Object -Maximum).Maximum
        $rng = $sel.Range
        $t = $doc.Tables.Add($rng, $rows.Count, $nc)
        $t.Borders.Enable = $true
        $t.Range.Font.Name = ''
        $t.Range.Font.Size = 9
        for ($ri = 0; $ri -lt $rows.Count; $ri++) {
          for ($ci = 0; $ci -lt $rows[$ri].Count; $ci++) {
            $cellTxt = $rows[$ri][$ci]
            $t.Cell($ri + 1, $ci + 1).Range.Text = $cellTxt
          }
          if ($ri -eq 0) { $t.Rows.Item(1).Range.Font.Bold = $true }
        }
        # 移到表格后
        $doc.Application.Selection.EndKey(6) | Out-Null  # wdStory
      }
      $sel.Font.Size = 10.5
      continue
    }

    # 标题
    if ($line -match '^(#{1,6})\s+(.*)') {
      $level = $matches[1].Length
      $txt = Clean-Inline $matches[2]
      switch ($level) {
        1 { $sel.Style = $wdStyleTitle }
        2 { $sel.Style = $wdStyleHeading1 }
        3 { $sel.Style = $wdStyleHeading2 }
        default { $sel.Style = $wdStyleHeading3 }
      }
      $sel.TypeText($txt)
      $sel.TypeParagraph()
      $sel.Style = $wdStyleNormal
      $i++; continue
    }

    # 分隔线
    if ($line -match '^\s*---+\s*$' -or $line -match '^\s*===+\s*$') { $i++; continue }

    # 引用
    if ($line -match '^\s*>\s?(.*)') {
      $sel.Style = $wdStyleNormal
      $sel.Font.Italic = $true
      $sel.TypeText((Clean-Inline $matches[1]))
      $sel.TypeParagraph()
      $sel.Font.Italic = $false
      $i++; continue
    }

    # 列表项
    if ($line -match '^\s*[-*]\s+(.*)') {
      $sel.Style = $wdStyleNormal
      $sel.TypeText(('• ' + (Clean-Inline $matches[1])))
      $sel.TypeParagraph()
      $i++; continue
    }
    if ($line -match '^\s*(\d+)\.\s+(.*)') {
      $sel.Style = $wdStyleNormal
      $sel.TypeText(($matches[1] + '. ' + (Clean-Inline $matches[2])))
      $sel.TypeParagraph()
      $i++; continue
    }

    # 空行
    if ($line.Trim() -eq '') { $sel.TypeParagraph(); $i++; continue }

    # 普通段落
    $sel.Style = $wdStyleNormal
    $sel.TypeText((Clean-Inline $line))
    $sel.TypeParagraph()
    $i++
  }

  if ($inCode) { Flush-Code $codeBuf }

  $doc.SaveAs([ref]$Out, [ref]$wdFormatDocx)
  Write-Host "已生成: $Out"
}
finally {
  $word.Quit()
  [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
}
