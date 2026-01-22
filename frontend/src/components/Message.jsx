import React, { useState } from 'react';
import { formatMessage, escapeHtml } from '../utils/markdown';

const MIN_RELEVANCE = 0.8;

export function Message({ role, content, sources = [], followUps = [], onFollowUpClick }) {
    const [copied, setCopied] = useState(false);

    const filteredSources = sources.filter(s => s.score >= MIN_RELEVANCE);

    const handleCopy = async () => {
        try {
            await navigator.clipboard.writeText(content);
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        } catch (err) {
            console.error('Failed to copy:', err);
        }
    };

    return (
        <div className={`message ${role}`}>
            <div className="message-content">
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
                                        {source.title} <span className="relevance">{relevance}%)</span>
                                    </a>
                                );
                            })}
                        </div>
                    )}
                    {followUps && followUps.length > 0 && (
                        <div className="follow-ups">
                            <div className="follow-ups-label">Follow-up questions:</div>
                            <div className="follow-ups-list">
                                {followUps.map((question, index) => (
                                    <button
                                        key={index}
                                        className="follow-up-btn"
                                        onClick={() => onFollowUpClick && onFollowUpClick(question)}
                                    >
                                        {question}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
                {role === 'assistant' && (
                    <button
                        className={`copy-btn ${copied ? 'copied' : ''}`}
                        onClick={handleCopy}
                        title={copied ? 'Copied!' : 'Copy message'}
                        aria-label={copied ? 'Copied!' : 'Copy message'}
                    >
                        {copied ? (
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        ) : (
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                            </svg>
                        )}
                    </button>
                )}
            </div>
        </div>
    );
}
