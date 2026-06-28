function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function renderInline(text) {
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>');
}

function markdownToHtml(markdown) {
  const normalized = escapeHtml(markdown || '').replace(/\r\n/g, '\n');
  const lines = normalized.split('\n');
  const blocks = [];
  let inList = false;

  lines.forEach((line) => {
    if (!line.trim()) {
      if (inList) {
        blocks.push('</ul>');
        inList = false;
      }
      return;
    }

    const headingMatch = line.match(/^(#{1,3})\s+(.+)$/);
    if (headingMatch) {
      if (inList) {
        blocks.push('</ul>');
        inList = false;
      }
      const level = headingMatch[1].length;
      blocks.push(`<h${level}>${renderInline(headingMatch[2])}</h${level}>`);
      return;
    }

    const listMatch = line.match(/^[-*]\s+(.+)$/);
    if (listMatch) {
      if (!inList) {
        blocks.push('<ul>');
        inList = true;
      }
      blocks.push(`<li>${renderInline(listMatch[1])}</li>`);
      return;
    }

    if (inList) {
      blocks.push('</ul>');
      inList = false;
    }
    blocks.push(`<p>${renderInline(line)}</p>`);
  });

  if (inList) {
    blocks.push('</ul>');
  }

  return blocks.join('');
}

module.exports = {
  markdownToHtml,
};
