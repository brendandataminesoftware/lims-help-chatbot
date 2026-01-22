import { marked } from 'marked';
import DOMPurify from 'dompurify';

// Configure marked options
marked.setOptions({
    breaks: true,        // Convert \n to <br>
    gfm: true,           // GitHub Flavored Markdown
    headerIds: false,    // Don't add ids to headers
    mangle: false,       // Don't mangle email addresses
});

// Custom renderer for code blocks with syntax highlighting class
const renderer = new marked.Renderer();

renderer.code = function(code, language) {
    const langClass = language ? ` language-${language}` : '';
    const codeText = typeof code === 'object' ? code.text : code;
    const lang = typeof code === 'object' ? code.lang : language;
    const actualLangClass = lang ? ` language-${lang}` : '';
    return `<pre><code class="code-block${actualLangClass}">${escapeHtml(codeText)}</code></pre>`;
};

renderer.table = function(header, body) {
    const headerContent = typeof header === 'object' ? header.header : header;
    const bodyContent = typeof header === 'object' ? header.rows?.map(row =>
        `<tr>${row.map(cell => `<td>${cell.text}</td>`).join('')}</tr>`
    ).join('') : body;

    if (typeof header === 'object') {
        const headerRow = header.header?.map(cell => `<th>${cell.text}</th>`).join('') || '';
        const rows = header.rows?.map(row =>
            `<tr>${row.map(cell => `<td>${cell.text}</td>`).join('')}</tr>`
        ).join('') || '';
        return `<table class="markdown-table"><thead><tr class="table-header">${headerRow}</tr></thead><tbody>${rows}</tbody></table>`;
    }
    return `<table class="markdown-table"><thead>${headerContent}</thead><tbody>${bodyContent}</tbody></table>`;
};

marked.use({ renderer });

export function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

export function formatMessage(text) {
    if (!text) return '';

    // Parse markdown to HTML using marked
    const rawHtml = marked.parse(text);

    // Sanitize the HTML to prevent XSS attacks
    const cleanHtml = DOMPurify.sanitize(rawHtml, {
        ALLOWED_TAGS: [
            'p', 'br', 'strong', 'em', 'code', 'pre', 'blockquote',
            'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
            'ul', 'ol', 'li',
            'a', 'img',
            'table', 'thead', 'tbody', 'tr', 'th', 'td',
            'hr', 'del', 's',
            'sup', 'sub',
            'span', 'div'
        ],
        ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'target', 'rel'],
        ADD_ATTR: ['target'],
    });

    // Add target="_blank" to all links for security
    const withSafeLinks = cleanHtml.replace(
        /<a /g,
        '<a target="_blank" rel="noopener noreferrer" '
    );

    return withSafeLinks;
}
