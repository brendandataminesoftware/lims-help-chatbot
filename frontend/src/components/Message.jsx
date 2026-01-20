import React from 'react';
import { formatMessage, escapeHtml } from '../utils/markdown';

const MIN_RELEVANCE = 0.8;

export function Message({ role, content, sources = [] }) {
    const avatar = role === 'user' ? 'You' : 'Alfred';
    const avatarClass = role === 'user' ? 'user' : 'assistant';

    const filteredSources = sources.filter(s => s.score >= MIN_RELEVANCE);

    return (
        <div className={`message ${role}`}>
            <div className="message-content">
                <div className={`avatar ${avatarClass}`}>{avatar}</div>
                <div className="message-text">
                    <div dangerouslySetInnerHTML={{ __html: formatMessage(content) }} />
                    {filteredSources.length > 0 && (
                        <div className="sources">
                            <div className="sources-label">Sources:</div>
                            {filteredSources.map((source, index) => {
                                const relevance = Math.round(source.score * 100);
                                return (
                                    <a
                                        key={index}
                                        href={source.url}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="source-link"
                                    >
                                        {source.title} <span className="relevance">({relevance}%)</span>
                                    </a>
                                );
                            })}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
