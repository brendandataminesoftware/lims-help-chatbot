export function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function convertTableToHtml(tableLines) {
    if (tableLines.length < 2) return tableLines.join('\n');

    let html = '<table class="markdown-table">';

    for (let i = 0; i < tableLines.length; i++) {
        const line = tableLines[i];

        // Skip separator row (contains only |, -, :, and spaces)
        if (/^\|[\s\-:|]+\|$/.test(line)) {
            continue;
        }

        // Parse cells
        const cells = line
            .slice(1, -1)
            .split('|')
            .map(cell => cell.trim());

        const tag = i === 0 ? 'th' : 'td';
        const rowClass = i === 0 ? 'table-header' : '';

        html += `<tr class="${rowClass}">`;
        for (const cell of cells) {
            html += `<${tag}>${cell}</${tag}>`;
        }
        html += '</tr>';
    }

    html += '</table>';
    return html;
}

function parseMarkdownTables(text) {
    const lines = text.split('\n');
    let result = [];
    let tableLines = [];
    let inTable = false;

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        const isTableRow = line.startsWith('|') && line.endsWith('|');

        if (isTableRow) {
            if (!inTable) {
                inTable = true;
                tableLines = [];
            }
            tableLines.push(line);
        } else {
            if (inTable) {
                result.push(convertTableToHtml(tableLines));
                tableLines = [];
                inTable = false;
            }
            result.push(lines[i]);
        }
    }

    if (inTable && tableLines.length > 0) {
        result.push(convertTableToHtml(tableLines));
    }

    return result.join('\n');
}

export function formatMessage(text) {
    // Escape HTML first
    let formatted = escapeHtml(text);

    // Code blocks
    formatted = formatted.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>');

    // Inline code
    formatted = formatted.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Bold
    formatted = formatted.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

    // Italic
    formatted = formatted.replace(/\*([^*]+)\*/g, '<em>$1</em>');

    // Parse markdown tables
    formatted = parseMarkdownTables(formatted);

    // Line breaks to paragraphs
    formatted = formatted.split('\n\n').map(p => `<p>${p}</p>`).join('');
    formatted = formatted.replace(/\n/g, '<br>');

    return formatted;
}
